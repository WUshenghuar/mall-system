package com.mall.trade.service.impl;

import com.mall.trade.entity.TradePay;
import com.mall.trade.mapper.TradePayMapper;
import com.mall.trade.service.PayService;
import com.mall.trade.service.TradeOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/** 管理支付记录，验证网关回调签名和金额并协调订单支付状态。 */
/**
 * 支付服务实现类
 * 提供支付相关的业务逻辑实现，包括创建支付单、查询支付状态、处理支付回调等功能
 */
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    // 支付数据访问层
    private final TradePayMapper payMapper;
    // 订单服务
    private final TradeOrderService tradeOrderService;
    // 是否启用模拟支付
    @Value("${trade.payment.simulation-enabled:false}")
    private boolean simulationEnabled;
    // 支付宝公钥
    @Value("${trade.payment.alipay.public-key:}")
    private String alipayPublicKey;

    /**
     * 生成支付单号
     * @return 返回生成的支付单号，格式为"P" + 31位UUID
     */
    private String generatePayNo() {
        return "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
    }

    /** 锁定并校验待支付订单；若已有未完成支付单则复用，避免重复创建支付记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradePay createPay(String orderNo, Integer payType, Long userId) {
        if (!Integer.valueOf(1).equals(payType)) {
            throw new com.mall.common.exception.BusinessException("支付方式不支持");
        }
        var order = tradeOrderService.getOwnedByOrderNoForUpdate(orderNo, userId);
        if (order.getOrderStatus() != 0) {
            throw new com.mall.common.exception.BusinessException("当前订单不可支付");
        }
        TradePay existing = findLatestPay(orderNo);
        if (existing != null && existing.getPayStatus() == 0) return existing;
        TradePay pay = new TradePay();
        pay.setOrderNo(orderNo);
        pay.setPayNo(generatePayNo());
        pay.setPayType(payType);
        pay.setPayStatus(0);
        pay.setPayAmount(order.getPayAmount());
        payMapper.insert(pay);
        return pay;
    }

    @Override
    public TradePay getPayStatus(String orderNo, Long userId) {
        tradeOrderService.getOwnedByOrderNo(orderNo, userId);
        return findLatestPay(orderNo);
    }

    /**
     * 根据订单号查询最新的支付记录
     * @param orderNo 订单号
     * @return 返回最新的支付记录对象，如果没有找到则返回null
     */
    private TradePay findLatestPay(String orderNo) {
        // 使用MyBatis-Plus的LambdaQueryWrapper构建查询条件
        // 1. 查询TradePay表
        // 2. 筛选条件：订单号等于传入的orderNo
        // 3. 按创建时间降序排序，确保获取的是最新的记录
        // 4. 使用LIMIT 1限制只返回一条记录
        return payMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(TradePay.class)
                        .eq(TradePay::getOrderNo, orderNo)
                        .orderByDesc(TradePay::getCreateTime)
                        .last("LIMIT 1")
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void simulateSuccess(String payNo, Long userId) {
        if (!simulationEnabled) throw new com.mall.common.exception.BusinessException("模拟支付未启用");
        TradePay pay = payMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(TradePay.class)
                .eq(TradePay::getPayNo, payNo).last("LIMIT 1"));
        if (pay == null) throw new com.mall.common.exception.BusinessException("支付记录不存在");
        tradeOrderService.getOwnedByOrderNo(pay.getOrderNo(), userId);
        completePayment(pay, "SIMULATED_SUCCESS");
    }

    /** 先验签并比对支付金额，再推进支付状态；已成功的重复通知视为已处理。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlipayNotify(Map<String, String> params) {
        String orderNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) || !hasValidAlipaySignature(params) || orderNo == null || orderNo.isBlank()) {
            return false;
        }
        TradePay pay = findLatestPay(orderNo);
        var order = tradeOrderService.getByOrderNo(orderNo);
        BigDecimal callbackAmount = parseAmount(params.get("total_amount"));
        if (pay == null || order == null || !orderNo.equals(pay.getOrderNo()) || !orderNo.equals(order.getOrderNo()) || pay.getPayType() == null
                || pay.getPayType() != 1 || callbackAmount == null || !sameAmount(callbackAmount, pay.getPayAmount())
                || !sameAmount(callbackAmount, order.getPayAmount())) {
            return false;
        }
        if (pay.getPayStatus() != null && pay.getPayStatus() == 1) {
            return true;
        }
        return completePayment(pay, params.toString());
    }

    private boolean completePayment(TradePay pay, String callbackContent) {
        if (payMapper.markSuccess(pay.getId(), callbackContent) != 1) {
            TradePay current = payMapper.selectByIdForUpdate(pay.getId());
            return current != null && Integer.valueOf(1).equals(current.getPayStatus());
        }
        if (!tradeOrderService.markPaid(pay.getOrderNo(), pay.getPayType())) {
            throw new com.mall.common.exception.BusinessException("订单支付状态异常");
        }
        return true;
    }

    /**
     * 验证支付宝签名是否有效
     * @param params 包含支付宝请求参数的Map，必须包含sign和sign_type参数
     * @return 如果签名验证通过返回true，否则返回false
     */
    private boolean hasValidAlipaySignature(Map<String, String> params) {
        // 从参数中获取签名值
        String signature = params.get("sign");
        // 检查签名类型是否为RSA2，签名是否存在且不为空，以及支付宝公钥是否存在且不为空
        if (!"RSA2".equals(params.get("sign_type")) || signature == null || signature.isBlank()
                || alipayPublicKey == null || alipayPublicKey.isBlank()) return false;
        try {
            // 移除公钥字符串中的所有换行符和BEGIN/END标记
            String publicKey = alipayPublicKey.replaceAll("-----BEGIN (.*)-----|-----END (.*)-----|\\s", "");
            // 使用Base64解码公钥，并生成RSA公钥对象
            var key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
            // 创建SHA256withRSA签名验证器
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(signingContent(params).getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (Exception ignored) {
            return false;
        }
    }

    private String signingContent(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(entry -> !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    /**
     * 将字符串金额解析为BigDecimal对象
     * @param amount 要解析的金额字符串
     * @return 解析成功返回BigDecimal对象，解析失败或输入为null返回null
     */
    private BigDecimal parseAmount(String amount) {
        try {
            // 使用三元运算符处理null值，如果是null则返回null，否则尝试转换为BigDecimal
            return amount == null ? null : new BigDecimal(amount);
        } catch (NumberFormatException ignored) {
            // 捕获数字格式异常，忽略异常信息，返回null
            return null;
        }
    }

    private boolean sameAmount(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }
}
