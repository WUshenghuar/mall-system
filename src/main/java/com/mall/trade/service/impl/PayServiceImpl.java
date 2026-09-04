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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final TradePayMapper payMapper;
    private final TradeOrderService tradeOrderService;
    @Value("${trade.payment.simulation-enabled:false}")
    private boolean simulationEnabled;

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
    public void handleAlipayNotify(Map<String, String> params) {
        String orderNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            TradePay pay = findLatestPay(orderNo);
            if (pay != null) {
                completePayment(pay, params.toString());
            }
        }
    }

    @Override
    public void handleWechatNotify(Map<String, String> params) {
        // 微信回调处理逻辑
    }

    private void completePayment(TradePay pay, String callbackContent) {
        if (pay.getPayStatus() != 0) return;
        if (tradeOrderService.markPaid(pay.getOrderNo(), pay.getPayType())) {
            payMapper.markSuccess(pay.getId(), callbackContent);
        }
    }
}
