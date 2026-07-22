package com.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeOrderItem;
import com.mall.trade.mapper.TradeOrderItemMapper;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.service.TradeOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TradeOrderServiceImpl implements TradeOrderService {

    private final TradeOrderMapper orderMapper;
    private final TradeOrderItemMapper orderItemMapper;

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "T" + date + rand;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeOrder createOrder(Long userId, Long addressId, Long couponId, String remark, String itemsJson) {
        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setOrderStatus(0);
        order.setRemark(remark);
        order.setSourceType(1);
        order.setPayAmount(BigDecimal.ZERO);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        // TODO: 解析 itemsJson → 校验库存 → 计算金额 → 创建订单明细
        orderMapper.insert(order);
        return order;
    }

    @Override
    public IPage<TradeOrder> selectPage(Integer page, Integer size, Long userId, Integer orderStatus) {
        LambdaQueryWrapper<TradeOrder> qw = Wrappers.lambdaQuery(TradeOrder.class);
        if (userId != null) qw.eq(TradeOrder::getUserId, userId);
        if (orderStatus != null) qw.eq(TradeOrder::getOrderStatus, orderStatus);
        qw.orderByDesc(TradeOrder::getCreateTime);
        return orderMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public TradeOrder getByOrderNo(String orderNo) {
        LambdaQueryWrapper<TradeOrder> qw = Wrappers.lambdaQuery(TradeOrder.class)
                .eq(TradeOrder::getOrderNo, orderNo);
        return orderMapper.selectOne(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        TradeOrder order = getByOrderNo(orderNo);
        if (order == null) throw new BusinessException("订单不存在");
        if (order.getOrderStatus() != 0) throw new BusinessException("当前状态不可取消");
        order.setOrderStatus(4);
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(String orderNo, Long userId) {
        TradeOrder order = getByOrderNo(orderNo);
        if (order == null) throw new BusinessException("订单不存在");
        if (order.getOrderStatus() != 2) throw new BusinessException("当前状态不可确认收货");
        order.setOrderStatus(3);
        orderMapper.updateById(order);
    }
}
