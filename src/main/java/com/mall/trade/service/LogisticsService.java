package com.mall.trade.service;

import com.mall.trade.entity.TradeLogistics;

public interface LogisticsService {
    TradeLogistics getByOrderNo(String orderNo);
    void ship(String orderNo, String logisticsNo, String logisticsCompany);
    void updateLogisticsInfo(String orderNo, String logisticsInfo);
}
