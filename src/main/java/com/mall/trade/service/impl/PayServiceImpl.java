package com.mall.trade.service.impl;

import com.mall.trade.entity.TradePay;
import com.mall.trade.mapper.TradePayMapper;
import com.mall.trade.service.PayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final TradePayMapper payMapper;

    private String generatePayNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "P" + date + rand;
    }

    @Override
    public TradePay createPay(String orderNo, Integer payType) {
        TradePay pay = new TradePay();
        pay.setOrderNo(orderNo);
        pay.setPayNo(generatePayNo());
        pay.setPayType(payType);
        pay.setPayStatus(0);
        pay.setPayAmount(BigDecimal.ZERO);
        payMapper.insert(pay);
        return pay;
    }

    @Override
    public TradePay getPayStatus(String orderNo) {
        return payMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(TradePay.class)
                        .eq(TradePay::getOrderNo, orderNo)
                        .orderByDesc(TradePay::getCreateTime)
                        .last("LIMIT 1")
        );
    }

    @Override
    public void handleAlipayNotify(Map<String, String> params) {
        String orderNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            TradePay pay = getPayStatus(orderNo);
            if (pay != null) {
                pay.setPayStatus(1);
                pay.setPayTime(LocalDateTime.now());
                pay.setCallbackContent(params.toString());
                pay.setCallbackTime(LocalDateTime.now());
                payMapper.updateById(pay);
            }
        }
    }

    @Override
    public void handleWechatNotify(Map<String, String> params) {
        // 微信回调处理逻辑
    }
}
