package com.mall.trade.service;

import com.mall.trade.entity.TradePay;

import java.util.Map;

public interface PayService {
    TradePay createPay(String orderNo, Integer payType, Long userId);
    TradePay getPayStatus(String orderNo, Long userId);
    void simulateSuccess(String payNo, Long userId);
    void handleAlipayNotify(Map<String, String> params);
    void handleWechatNotify(Map<String, String> params);
}
