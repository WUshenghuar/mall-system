package com.mall.trade.service;

import com.mall.trade.entity.TradeLogistics;

public interface LogisticsService {
/**
 * 根据订单号获取物流信息
 * @param orderNo 订单号，用于查询对应的物流信息
 * @return 返回与订单号匹配的物流信息对象，如果未找到则可能返回null
 */
    TradeLogistics getByOrderNo(String orderNo);
    void ship(String orderNo, String logisticsNo, String logisticsCompany);
    void updateLogisticsInfo(String orderNo, String logisticsInfo);
}
