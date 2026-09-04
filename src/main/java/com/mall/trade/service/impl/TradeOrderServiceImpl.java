package com.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.exception.BusinessException;
import com.mall.member.entity.MemberAddress;
import com.mall.member.mapper.MemberAddressMapper;
import com.mall.product.entity.Sku;
import com.mall.product.entity.SkuStock;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SkuStockMapper;
import com.mall.trade.entity.TradeCart;
import com.mall.trade.entity.TradeLogistics;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeOrderItem;
import com.mall.trade.mapper.TradeCartMapper;
import com.mall.trade.mapper.TradeLogisticsMapper;
import com.mall.trade.mapper.TradeOrderItemMapper;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.mq.TradeEventPublisher;
import com.mall.trade.service.TradeOrderService;
import com.mall.trade.service.RedisStockReservationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeOrderServiceImpl implements TradeOrderService {

    private final TradeOrderMapper orderMapper;
    private final TradeOrderItemMapper orderItemMapper;
    private final TradeCartMapper cartMapper;
    private final TradeLogisticsMapper logisticsMapper;
    private final SkuMapper skuMapper;
    private final SkuStockMapper skuStockMapper;
    private final MemberAddressMapper addressMapper;
    private final ObjectMapper objectMapper;
    private final TradeEventPublisher tradeEventPublisher;
    private final RedisStockReservationService redisStockReservationService;

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "T" + date + rand;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TradeOrder createOrder(Long userId, Long addressId, Long couponId, String remark, String itemsJson) {
        // 1. 解析 itemsJson: [{"skuId":1,"quantity":2}, ...]
        List<OrderItemReq> items;
        try {
            items = objectMapper.readValue(itemsJson, new TypeReference<List<OrderItemReq>>() {});
        } catch (Exception e) {
            throw new BusinessException("下单参数格式错误");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException("下单商品不能为空");
        }

        // 2. 校验每项 quantity 合法性
        for (OrderItemReq item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("商品数量不合法");
            }
        }

        // 3. 查询收货地址
        MemberAddress address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException("收货地址不存在");
        }
        if (!userId.equals(address.getUserId())) {
            throw new BusinessException("无权使用该收货地址");
        }

        // 4. 批量查询 SKU 和库存（避免 N+1）
        List<Long> skuIds = items.stream().map(OrderItemReq::getSkuId).distinct().toList();
        if (skuIds.size() != items.size()) throw new BusinessException("下单商品不能重复");
        validateCheckedCart(userId, items, skuIds);
        Map<Long, Sku> skuMap = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));
        Map<Long, SkuStock> stockMap = skuStockMapper.selectList(
                Wrappers.<SkuStock>lambdaQuery().in(SkuStock::getSkuId, skuIds))
                .stream().collect(Collectors.toMap(SkuStock::getSkuId, s -> s));
        Map<Long, Integer> quantities = items.stream().collect(Collectors.toMap(OrderItemReq::getSkuId, OrderItemReq::getQuantity));
        Map<Long, Integer> availableStock = stockMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> entry.getValue().getStock() - entry.getValue().getLockedStock()));
        if (!redisStockReservationService.reserveAll(quantities, availableStock)) {
            throw new BusinessException("商品库存不足，请刷新后重试");
        }

        // 5. 校验商品 + 库存 + 锁定库存 + 计算金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<TradeOrderItem> orderItems = new ArrayList<>();

        for (OrderItemReq item : items) {
            Sku sku = skuMap.get(item.getSkuId());
            if (sku == null || !Integer.valueOf(1).equals(sku.getStatus())) {
                throw new BusinessException("商品[" + item.getSkuId() + "]不存在或已下架");
            }

            SkuStock stock = stockMap.get(item.getSkuId());
            int available = stock == null ? 0 : stock.getStock() - stock.getLockedStock();
            if (available < item.getQuantity()) {
                throw new BusinessException("商品[" + sku.getSkuCode() + "]库存不足，剩余" + available);
            }
            if (skuStockMapper.lockAvailableStock(item.getSkuId(), item.getQuantity()) != 1) {
                throw new BusinessException("商品[" + sku.getSkuCode() + "]库存不足，请刷新后重试");
            }

            BigDecimal itemTotal = sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            TradeOrderItem orderItem = new TradeOrderItem();
            orderItem.setSkuId(item.getSkuId());
            orderItem.setSkuName(sku.getSkuCode());
            orderItem.setSkuPrice(sku.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setTotalAmount(itemTotal.setScale(2, RoundingMode.HALF_UP));
            orderItem.setSkuImage(extractFirstImage(sku.getImages()));
            orderItems.add(orderItem);
        }

        // 6. 创建订单
        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setOrderStatus(0);
        order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setPayAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(
                address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
        order.setRemark(remark);
        order.setSourceType(1);
        orderMapper.insert(order);

        // 7. 插入订单明细
        for (TradeOrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItem.setOrderNo(order.getOrderNo());
            orderItemMapper.insert(orderItem);
        }

        // 8. 清空购物车已选商品
        cartMapper.delete(Wrappers.lambdaQuery(TradeCart.class)
                .eq(TradeCart::getUserId, userId)
                .eq(TradeCart::getChecked, 1)
                .in(TradeCart::getSkuId, skuIds));

        tradeEventPublisher.publish("created", order.getOrderNo());
        return order;
    }

    private String extractFirstImage(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) return null;
        try {
            List<String> images = objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {});
            return images.isEmpty() ? null : images.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    private static class OrderItemReq {
        private Long skuId;
        private Integer quantity;
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
    public TradeOrder getOwnedByOrderNo(String orderNo, Long userId) {
        TradeOrder order = getByOrderNo(orderNo);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException("订单不存在或无权访问");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo, Long userId) {
        TradeOrder order = getOwnedByOrderNo(orderNo, userId);
        if (orderMapper.transitionOwned(orderNo, userId, 0, 4) != 1) {
            throw new BusinessException("当前状态不可取消");
        }
        releaseLockedStock(orderNo);
        tradeEventPublisher.publish("cancelled", orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(String orderNo, Long userId) {
        TradeOrder order = getOwnedByOrderNo(orderNo, userId);
        if (orderMapper.transitionOwned(orderNo, userId, 2, 3) != 1) {
            throw new BusinessException("当前状态不可确认收货");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(String orderNo, Integer payType) {
        if (orderMapper.markPaid(orderNo, payType) != 1) {
            return false;
        }
        for (TradeOrderItem item : orderItems(orderNo)) {
            if (skuStockMapper.deductLockedStock(item.getSkuId(), item.getQuantity()) != 1) {
                throw new BusinessException("订单库存状态异常，请联系管理员");
            }
        }
        tradeEventPublisher.publish("paid", orderNo);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(String orderNo, String logisticsNo, String logisticsCompany) {
        if (orderMapper.markShipped(orderNo) != 1) {
            throw new BusinessException("当前状态不可发货");
        }
        TradeLogistics logistics = new TradeLogistics();
        logistics.setOrderNo(orderNo);
        logistics.setLogisticsNo(logisticsNo);
        logistics.setLogisticsCompany(logisticsCompany);
        logistics.setLogisticsStatus(1);
        logisticsMapper.insert(logistics);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelExpiredOrders() {
        List<TradeOrder> expired = orderMapper.selectList(Wrappers.<TradeOrder>lambdaQuery()
                .eq(TradeOrder::getOrderStatus, 0)
                .lt(TradeOrder::getCreateTime, LocalDateTime.now().minusMinutes(30)));
        int cancelled = 0;
        for (TradeOrder order : expired) {
            if (orderMapper.transitionOwned(order.getOrderNo(), order.getUserId(), 0, 4) == 1) {
                releaseLockedStock(order.getOrderNo());
                tradeEventPublisher.publish("cancelled", order.getOrderNo());
                cancelled++;
            }
        }
        return cancelled;
    }

    private List<TradeOrderItem> orderItems(String orderNo) {
        return orderItemMapper.selectList(Wrappers.<TradeOrderItem>lambdaQuery()
                .eq(TradeOrderItem::getOrderNo, orderNo));
    }

    private void validateCheckedCart(Long userId, List<OrderItemReq> items, List<Long> skuIds) {
        Map<Long, Integer> requested = items.stream().collect(Collectors.toMap(OrderItemReq::getSkuId, OrderItemReq::getQuantity));
        List<TradeCart> carts = cartMapper.selectList(Wrappers.<TradeCart>lambdaQuery()
                .eq(TradeCart::getUserId, userId).eq(TradeCart::getChecked, 1).in(TradeCart::getSkuId, skuIds));
        if (carts.size() != items.size() || carts.stream().anyMatch(cart -> !requested.get(cart.getSkuId()).equals(cart.getQuantity()))) {
            throw new BusinessException("下单商品与已复核购物车不一致");
        }
    }

    private void releaseLockedStock(String orderNo) {
        for (TradeOrderItem item : orderItems(orderNo)) {
            if (skuStockMapper.releaseLockedStock(item.getSkuId(), item.getQuantity()) != 1) {
                throw new BusinessException("订单库存状态异常，请联系管理员");
            }
            redisStockReservationService.release(item.getSkuId(), item.getQuantity());
        }
    }
}
