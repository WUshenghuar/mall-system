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
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeOrderItem;
import com.mall.trade.mapper.TradeCartMapper;
import com.mall.trade.mapper.TradeOrderItemMapper;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.service.TradeOrderService;
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
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TradeOrderServiceImpl implements TradeOrderService {

    private final TradeOrderMapper orderMapper;
    private final TradeOrderItemMapper orderItemMapper;
    private final TradeCartMapper cartMapper;
    private final SkuMapper skuMapper;
    private final SkuStockMapper skuStockMapper;
    private final MemberAddressMapper addressMapper;
    private final ObjectMapper objectMapper;

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

        // 2. 查询收货地址
        MemberAddress address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException("收货地址不存在");
        }

        // 3. 遍历商品：查 SKU、校验库存、计算金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<TradeOrderItem> orderItems = new ArrayList<>();

        for (OrderItemReq item : items) {
            Sku sku = skuMapper.selectById(item.getSkuId());
            if (sku == null || !Integer.valueOf(1).equals(sku.getStatus())) {
                throw new BusinessException("商品[" + item.getSkuId() + "]不存在或已下架");
            }

            SkuStock stock = skuStockMapper.selectOne(
                    Wrappers.lambdaQuery(SkuStock.class).eq(SkuStock::getSkuId, item.getSkuId()));
            int available = stock == null ? 0 : stock.getStock() - stock.getLockedStock();
            if (available < item.getQuantity()) {
                throw new BusinessException("商品[" + sku.getSkuCode() + "]库存不足");
            }
            // 锁定库存
            stock.setLockedStock(stock.getLockedStock() + item.getQuantity());
            skuStockMapper.updateById(stock);

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

        // 4. 创建订单
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

        // 5. 插入订单明细
        for (TradeOrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItem.setOrderNo(order.getOrderNo());
            orderItemMapper.insert(orderItem);
        }

        // 6. 清空购物车已选商品
        cartMapper.delete(Wrappers.lambdaQuery(TradeCart.class)
                .eq(TradeCart::getUserId, userId)
                .eq(TradeCart::getChecked, 1));

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
