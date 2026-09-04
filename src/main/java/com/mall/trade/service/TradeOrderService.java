package com.mall.trade.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.trade.entity.TradeOrder;

public interface TradeOrderService {
    TradeOrder createOrder(Long userId, Long addressId, Long couponId, String remark, String itemsJson);
    IPage<TradeOrder> selectPage(Integer page, Integer size, Long userId, Integer orderStatus);
    TradeOrder getByOrderNo(String orderNo);
    TradeOrder getOwnedByOrderNo(String orderNo, Long userId);
    void cancelOrder(String orderNo, Long userId);
    void confirmReceive(String orderNo, Long userId);
    boolean markPaid(String orderNo, Integer payType);
    void ship(String orderNo, String logisticsNo, String logisticsCompany);
    int cancelExpiredOrders();
}
