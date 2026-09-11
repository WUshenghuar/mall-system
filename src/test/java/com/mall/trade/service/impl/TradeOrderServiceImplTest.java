package com.mall.trade.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.exception.BusinessException;
import com.mall.finance.service.TaxConfigService;
import com.mall.marketing.entity.ActivitySku;
import com.mall.marketing.mapper.CouponIssueMapper;
import com.mall.marketing.service.ActivityService;
import com.mall.marketing.service.CouponService;
import com.mall.member.entity.MemberAddress;
import com.mall.member.mapper.MemberAddressMapper;
import com.mall.member.service.MemberService;
import com.mall.product.entity.Sku;
import com.mall.product.entity.SkuStock;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SkuStockMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.trade.entity.TradeCart;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeOrderItem;
import com.mall.trade.mapper.*;
import com.mall.trade.mq.TradeEventPublisher;
import com.mall.trade.service.RedisStockReservationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TradeOrderServiceImplTest {
    @Test
    void orderDetailRejectsAnotherMember() {
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        TradeOrder order = new TradeOrder();
        order.setOrderNo("T-1");
        order.setUserId(20L);
        when(orderMapper.selectOne(any())).thenReturn(order);
        TradeOrderServiceImpl service = new TradeOrderServiceImpl(orderMapper, mock(TradeOrderItemMapper.class),
                mock(TradeCartMapper.class), mock(TradeLogisticsMapper.class), mock(SkuMapper.class),
                mock(SkuStockMapper.class), mock(MemberAddressMapper.class), new ObjectMapper(),
                mock(TradeEventPublisher.class), mock(RedisStockReservationService.class),
                mock(CouponService.class), mock(MemberService.class), mock(CouponIssueMapper.class), mock(ActivityService.class),
                mock(SpuMapper.class), mock(TaxConfigService.class));

        assertThatThrownBy(() -> service.getOwnedByOrderNo("T-1", 10L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("无权访问");
    }

    @Test
    void createsOrderWithCouponAndMarksTheSameIssueUsed() {
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        TradeOrderItemMapper itemMapper = mock(TradeOrderItemMapper.class);
        TradeCartMapper cartMapper = mock(TradeCartMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        SkuStockMapper stockMapper = mock(SkuStockMapper.class);
        MemberAddressMapper addressMapper = mock(MemberAddressMapper.class);
        RedisStockReservationService reservation = mock(RedisStockReservationService.class);
        CouponService couponService = mock(CouponService.class);
        CouponIssueMapper issueMapper = mock(CouponIssueMapper.class);
        MemberAddress address = new MemberAddress();
        address.setUserId(9L); address.setReceiverName("张三"); address.setReceiverPhone("13800000000");
        address.setProvince("上海"); address.setCity("上海"); address.setDistrict("浦东"); address.setDetailAddress("陆家嘴");
        TradeCart cart = new TradeCart();
        cart.setUserId(9L); cart.setSkuId(7L); cart.setQuantity(2); cart.setChecked(1);
        Sku sku = new Sku();
        sku.setId(7L); sku.setSkuCode("SKU-7"); sku.setStatus(1); sku.setPrice(new BigDecimal("100.00"));
        SkuStock stock = new SkuStock();
        stock.setSkuId(7L); stock.setStock(10); stock.setLockedStock(0);
        when(addressMapper.selectById(5L)).thenReturn(address);
        when(cartMapper.selectList(any())).thenReturn(List.of(cart));
        when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku));
        when(stockMapper.selectList(any())).thenReturn(List.of(stock));
        when(reservation.reserveAll(any(), any())).thenReturn(true);
        when(stockMapper.lockAvailableStock(7L, 2)).thenReturn(1);
        when(couponService.validateAndCalculateDiscount(eq(9L), eq(99L), eq(new BigDecimal("200.00"))))
                .thenReturn(new CouponService.DiscountResult(new BigDecimal("20.00"), 11L));
        when(issueMapper.markUsed(eq(11L), eq(99L), eq(9L), anyString())).thenReturn(1);

        TradeOrderServiceImpl service = new TradeOrderServiceImpl(orderMapper, itemMapper, cartMapper,
                mock(TradeLogisticsMapper.class), skuMapper, stockMapper, addressMapper, new ObjectMapper(),
                mock(TradeEventPublisher.class), reservation, couponService, mock(MemberService.class), issueMapper, mock(ActivityService.class),
                mock(SpuMapper.class), mock(TaxConfigService.class));

        TradeOrder order = service.createOrder(9L, 5L, 99L, null, "[{\"skuId\":7,\"quantity\":2}]");

        assertThat(order.getTotalAmount()).isEqualByComparingTo("200.00");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("20.00");
        assertThat(order.getPayAmount()).isEqualByComparingTo("180.00");
        verify(issueMapper).markUsed(eq(11L), eq(99L), eq(9L), anyString());
    }

    @Test
    void createsOrderWithActiveActivityPriceAndReservesActivityStock() {
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        TradeOrderItemMapper itemMapper = mock(TradeOrderItemMapper.class);
        TradeCartMapper cartMapper = mock(TradeCartMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        SkuStockMapper stockMapper = mock(SkuStockMapper.class);
        MemberAddressMapper addressMapper = mock(MemberAddressMapper.class);
        RedisStockReservationService reservation = mock(RedisStockReservationService.class);
        CouponService couponService = mock(CouponService.class);
        ActivityService activityService = mock(ActivityService.class);
        MemberAddress address = new MemberAddress(); address.setUserId(9L);
        TradeCart cart = new TradeCart(); cart.setUserId(9L); cart.setSkuId(7L); cart.setQuantity(2); cart.setChecked(1);
        Sku sku = new Sku(); sku.setId(7L); sku.setSkuCode("SKU-7"); sku.setStatus(1); sku.setPrice(new BigDecimal("120.00"));
        SkuStock stock = new SkuStock(); stock.setSkuId(7L); stock.setStock(10); stock.setLockedStock(0);
        ActivitySku promotion = new ActivitySku(); promotion.setActivityId(8L); promotion.setSkuId(7L);
        promotion.setSeckillPrice(new BigDecimal("80.00")); promotion.setSeckillStock(5); promotion.setLimitPerUser(2);
        when(addressMapper.selectById(5L)).thenReturn(address);
        when(cartMapper.selectList(any())).thenReturn(List.of(cart));
        when(skuMapper.selectBatchIds(any())).thenReturn(List.of(sku));
        when(stockMapper.selectList(any())).thenReturn(List.of(stock));
        when(reservation.reserveAll(any(), any())).thenReturn(true);
        when(stockMapper.lockAvailableStock(7L, 2)).thenReturn(1);
        when(activityService.findActiveSku(7L)).thenReturn(promotion);
        when(activityService.reserveStock(promotion, 2)).thenReturn(true);

        TradeOrderServiceImpl service = new TradeOrderServiceImpl(orderMapper, itemMapper, cartMapper,
                mock(TradeLogisticsMapper.class), skuMapper, stockMapper, addressMapper, new ObjectMapper(),
                mock(TradeEventPublisher.class), reservation, couponService, mock(MemberService.class), mock(CouponIssueMapper.class), activityService,
                mock(SpuMapper.class), mock(TaxConfigService.class));

        TradeOrder order = service.createOrder(9L, 5L, null, null, "[{\"skuId\":7,\"quantity\":2}]");

        assertThat(order.getTotalAmount()).isEqualByComparingTo("160.00");
        verify(activityService).reserveStock(promotion, 2);
        verify(itemMapper).insert(argThat((com.mall.trade.entity.TradeOrderItem item) -> item.getActivityId().equals(8L)
                && item.getSkuPrice().compareTo(new BigDecimal("80.00")) == 0
                && Integer.valueOf(1).equals(item.getActivityStockReserved())));
    }

    @Test
    void refundRestoresDatabaseRedisAndActivityStock() {
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        TradeOrderItemMapper itemMapper = mock(TradeOrderItemMapper.class);
        SkuStockMapper stockMapper = mock(SkuStockMapper.class);
        RedisStockReservationService reservation = mock(RedisStockReservationService.class);
        ActivityService activityService = mock(ActivityService.class);
        TradeOrder order = new TradeOrder(); order.setOrderNo("T-1"); order.setUserId(9L); order.setOrderStatus(6);
        TradeOrderItem item = new TradeOrderItem(); item.setSkuId(7L); item.setQuantity(2); item.setActivityId(8L); item.setActivityStockReserved(1);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockMapper.restoreStock(7L, 2)).thenReturn(1);

        TradeOrderServiceImpl service = new TradeOrderServiceImpl(orderMapper, itemMapper, mock(TradeCartMapper.class),
                mock(TradeLogisticsMapper.class), mock(SkuMapper.class), stockMapper, mock(MemberAddressMapper.class), new ObjectMapper(),
                mock(TradeEventPublisher.class), reservation, mock(CouponService.class), mock(MemberService.class),
                mock(CouponIssueMapper.class), activityService, mock(SpuMapper.class), mock(TaxConfigService.class));

        service.restoreStockForRefund("T-1", 9L);

        verify(stockMapper).restoreStock(7L, 2);
        verify(reservation).release(7L, 2);
        verify(activityService).releaseStock(8L, 7L, 2);
    }

    @Test
    void unpaidOrderCancellationReleasesCouponIssue() {
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        TradeOrderItemMapper itemMapper = mock(TradeOrderItemMapper.class);
        CouponIssueMapper issueMapper = mock(CouponIssueMapper.class);
        TradeOrder order = new TradeOrder(); order.setOrderNo("T-2"); order.setUserId(9L); order.setOrderStatus(0);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.transitionOwned("T-2", 9L, 0, 4)).thenReturn(1);
        when(itemMapper.selectList(any())).thenReturn(List.of());
        when(issueMapper.releaseUsed(9L, "T-2")).thenReturn(1);
        TradeOrderServiceImpl service = new TradeOrderServiceImpl(orderMapper, itemMapper, mock(TradeCartMapper.class),
                mock(TradeLogisticsMapper.class), mock(SkuMapper.class), mock(SkuStockMapper.class), mock(MemberAddressMapper.class), new ObjectMapper(),
                mock(TradeEventPublisher.class), mock(RedisStockReservationService.class), mock(CouponService.class), mock(MemberService.class),
                issueMapper, mock(ActivityService.class), mock(SpuMapper.class), mock(TaxConfigService.class));

        service.cancelOrder("T-2", 9L);

        verify(issueMapper).releaseUsed(9L, "T-2");
    }

    @Test
    void expiredOrderCancellationReleasesCouponIssue() {
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        TradeOrderItemMapper itemMapper = mock(TradeOrderItemMapper.class);
        CouponIssueMapper issueMapper = mock(CouponIssueMapper.class);
        TradeOrder order = new TradeOrder(); order.setOrderNo("T-3"); order.setUserId(9L);
        order.setOrderStatus(0); order.setCreateTime(LocalDateTime.now().minusMinutes(31));
        when(orderMapper.selectList(any())).thenReturn(List.of(order));
        when(orderMapper.transitionOwned("T-3", 9L, 0, 4)).thenReturn(1);
        when(itemMapper.selectList(any())).thenReturn(List.of());
        when(issueMapper.releaseUsed(9L, "T-3")).thenReturn(1);
        TradeOrderServiceImpl service = new TradeOrderServiceImpl(orderMapper, itemMapper, mock(TradeCartMapper.class),
                mock(TradeLogisticsMapper.class), mock(SkuMapper.class), mock(SkuStockMapper.class), mock(MemberAddressMapper.class), new ObjectMapper(),
                mock(TradeEventPublisher.class), mock(RedisStockReservationService.class), mock(CouponService.class), mock(MemberService.class),
                issueMapper, mock(ActivityService.class), mock(SpuMapper.class), mock(TaxConfigService.class));

        assertThat(service.cancelExpiredOrders()).isEqualTo(1);
        verify(issueMapper).releaseUsed(9L, "T-3");
    }
}
