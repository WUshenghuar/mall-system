package com.mall.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.order.entity.Order;

import java.util.Map;

public interface OrderService {
    String createOrder(Long userId, Long skuId, Integer quantity, Long couponId);
    IPage<Order> selectPage(Integer page, Integer size, Integer orderStatus, String keyword);
    Order getById(Long id);
    Order getByOrderNo(String orderNo);
    /** MQ 消费者：处理订单创建消息 */
    void processOrderMessage(Map<String, Object> message);
    void paySuccess(String orderNo);
    void cancelOrder(Long id);
}
