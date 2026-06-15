package com.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mq.OrderMessageSender;
import com.mall.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private OrderMessageSender messageSender;

    public OrderServiceImpl(OrderMapper orderMapper,
                             OrderItemMapper orderItemMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    private static final String STOCK_KEY = "stock:sku:";
    private static final String LOCK_KEY = "lock:order:create:";

    @Override
    public String createOrder(Long userId, Long skuId, Integer quantity, Long couponId) {
        // 生成订单号
        String orderNo = "ORD-" + System.currentTimeMillis();

        // Redis 可用时：分布式锁 + 预扣库存
        if (redisTemplate != null) {
            String lockKey = LOCK_KEY + skuId + ":" + userId;
            String lockValue = UUID.randomUUID().toString();
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(locked)) {
                throw new RuntimeException("操作太频繁，请稍后重试");
            }
            try {
                Long stock = redisTemplate.opsForValue()
                        .decrement(STOCK_KEY + skuId);
                if (stock == null || stock < 0) {
                    redisTemplate.opsForValue().increment(STOCK_KEY + skuId);
                    throw new RuntimeException("库存不足");
                }
                sendCreateMessage(userId, skuId, quantity, couponId, orderNo);
            } finally {
                String val = (String) redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(val)) {
                    redisTemplate.delete(lockKey);
                }
            }
        } else {
            // 无 Redis：直接同步创建
            sendCreateMessage(userId, skuId, quantity, couponId, orderNo);
        }
        return orderNo;
    }

    private void sendCreateMessage(Long userId, Long skuId, Integer quantity,
                                    Long couponId, String orderNo) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", userId);
        msg.put("skuId", skuId);
        msg.put("quantity", quantity);
        msg.put("couponId", couponId);
        msg.put("orderNo", orderNo);

        if (messageSender != null) {
            messageSender.sendOrderCreate(msg);
        } else {
            processOrderMessage(msg);
        }
    }

    @Override
    @Transactional
    public void processOrderMessage(Map<String, Object> msg) {
        String orderNo = (String) msg.get("orderNo");
        Long userId = Long.valueOf(msg.get("userId").toString());
        Long skuId = Long.valueOf(msg.get("skuId").toString());
        Integer quantity = Integer.valueOf(msg.get("quantity").toString());

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(new BigDecimal("99.99").multiply(BigDecimal.valueOf(quantity)));
        order.setCurrency("USD");
        order.setExchangeRate(new BigDecimal("7.24"));
        order.setTariffAmount(new BigDecimal("9.99"));
        order.setShippingFee(new BigDecimal("15.00"));
        order.setPayAmount(order.getTotalAmount().add(order.getTariffAmount()).add(order.getShippingFee()));
        order.setOrderStatus(0);
        order.setPayStatus(0);
        order.setLogisticsStatus(0);
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setPrice(new BigDecimal("99.99"));
        item.setTotalPrice(new BigDecimal("99.99").multiply(BigDecimal.valueOf(quantity)));
        orderItemMapper.insert(item);

        log.info("Order created successfully: {}", orderNo);
    }

    @Override
    @Transactional
    public void paySuccess(String orderNo) {
        Order order = orderMapper.selectOne(
                Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
        if (order == null || order.getOrderStatus() != 0) return;

        order.setOrderStatus(1);
        order.setPayStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order paid: {}", orderNo);
    }

    @Override
    public IPage<Order> selectPage(Integer page, Integer size,
                                    Integer orderStatus, String keyword) {
        LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
                .eq(orderStatus != null, Order::getOrderStatus, orderStatus)
                .like(StringUtils.hasText(keyword), Order::getOrderNo, keyword)
                .orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return orderMapper.selectOne(
                Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
    }

    @Override
    public void cancelOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        order.setOrderStatus(5);
        orderMapper.updateById(order);
    }
}
