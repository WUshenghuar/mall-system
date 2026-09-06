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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final TradePayMapper payMapper;
    private final TradeOrderService tradeOrderService;
    @Value("${trade.payment.simulation-enabled:false}")
    private boolean simulationEnabled;
    @Value("${trade.payment.alipay.public-key:}")
    private String alipayPublicKey;

    private String generatePayNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "P" + date + rand;
    }

    @Override
    public TradePay createPay(String orderNo, Integer payType, Long userId) {
        var order = tradeOrderService.getOwnedByOrderNo(orderNo, userId);
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

    private TradePay findLatestPay(String orderNo) {
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

    @Override
    public void handleWechatNotify(Map<String, String> params) {
        // 微信回调处理逻辑
    }

    private boolean completePayment(TradePay pay, String callbackContent) {
        if (payMapper.markSuccess(pay.getId(), callbackContent) != 1) {
            return false;
        }
        if (!tradeOrderService.markPaid(pay.getOrderNo(), pay.getPayType())) {
            throw new com.mall.common.exception.BusinessException("订单支付状态异常");
        }
        return true;
    }

    private boolean hasValidAlipaySignature(Map<String, String> params) {
        String signature = params.get("sign");
        if (!"RSA2".equals(params.get("sign_type")) || signature == null || signature.isBlank()
                || alipayPublicKey == null || alipayPublicKey.isBlank()) return false;
        try {
            String publicKey = alipayPublicKey.replaceAll("-----BEGIN (.*)-----|-----END (.*)-----|\\s", "");
            var key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
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

    private BigDecimal parseAmount(String amount) {
        try {
            return amount == null ? null : new BigDecimal(amount);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean sameAmount(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }
}
