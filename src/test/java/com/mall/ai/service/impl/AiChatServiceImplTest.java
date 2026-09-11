package com.mall.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.entity.AiConversation;
import com.mall.ai.mapper.AiConversationMapper;
import com.mall.ai.service.AiGatewayClient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.service.LogisticsService;
import com.mall.trade.service.TradeOrderService;
import com.mall.trade.service.TradeRefundService;
import com.mall.product.service.StoreCatalogService;
import com.mall.product.entity.Spu;
import com.mall.marketing.entity.MemberCouponVO;
import com.mall.marketing.service.CouponService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatServiceImplTest {
    @Test
    void streamPersistsBothSidesOfConversationAndForwardsTextEvent() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        TradeOrderService orderService = mock(TradeOrderService.class);
        LogisticsService logisticsService = mock(LogisticsService.class);
        TradeRefundService refundService = mock(TradeRefundService.class);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(6);
            consumer.accept("{\"type\":\"text\",\"content\":\"你好\"}");
            return null;
        }).when(gateway).stream(anyLong(), anyString(), anyString(), anyString(), anyString(), any(), any());
        AiChatRequest request = new AiChatRequest();
        request.setMessage("你好");
        List<String> events = new ArrayList<>();

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, logisticsService, refundService, mock(StoreCatalogService.class), mock(CouponService.class))
                .stream(9L, "session-1", request, events::add);

        verify(mapper, org.mockito.Mockito.times(2)).insert(any(AiConversation.class));
        assertThat(events).containsExactly("{\"type\":\"text\",\"content\":\"你好\"}");
    }

    @Test
    void streamPassesOwnedOrderStatusAsReadOnlyContext() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        TradeOrderService orderService = mock(TradeOrderService.class);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        TradeOrder order = new TradeOrder(); order.setOrderStatus(2); order.setPayAmount(new java.math.BigDecimal("19.90"));
        when(orderService.getOwnedByOrderNo("T202609071234567890", 9L)).thenReturn(order);
        AiChatRequest request = new AiChatRequest(); request.setMessage("查询订单 T202609071234567890");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(), eq("订单T202609071234567890当前状态：待收货，实付金额：19.90。"), eq("query_order"), any(), any());
    }

    @Test
    void streamQueriesRecentOwnedOrdersWhenOrderNumberIsMissing() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        TradeOrderService orderService = mock(TradeOrderService.class);
        IPage<TradeOrder> page = mock(IPage.class);
        TradeOrder order = new TradeOrder(); order.setOrderNo("T202609071234567890"); order.setOrderStatus(1);
        order.setPayAmount(new java.math.BigDecimal("19.90"));
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(orderService.selectPage(1, 3, 9L, null)).thenReturn(page);
        when(page.getRecords()).thenReturn(List.of(order));
        AiChatRequest request = new AiChatRequest(); request.setMessage("查询我的订单");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(), eq("你最近的订单：\n订单T202609071234567890：待发货，实付金额：19.90"), eq("query_order"), any(), any());
    }

    @Test
    void streamQueriesPublishedProductsAsReadOnlyContext() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        StoreCatalogService catalog = mock(StoreCatalogService.class);
        IPage page = mock(IPage.class);
        Spu product = new Spu(); product.setSpuName("跨境耳机");
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(catalog.products(1, 3, null, "耳机")).thenReturn(page);
        when(page.getRecords()).thenReturn(List.of(product));
        AiChatRequest request = new AiChatRequest(); request.setMessage("查询耳机商品");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), catalog, mock(CouponService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(), eq("为你找到的上架商品：\n跨境耳机"), eq("query_product"), any(), any());
    }

    @Test
    void streamLeavesRefundRulesForKnowledgeRetrieval() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        AiChatRequest request = new AiChatRequest(); request.setMessage("退款规则是什么");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(), eq(""), eq(""), any(), any());
    }

    @Test
    void streamQueriesOnlyAvailableMemberCoupons() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        CouponService coupons = mock(CouponService.class);
        MemberCouponVO coupon = new MemberCouponVO(); coupon.setCouponName("满100减20");
        coupon.setDiscount(new java.math.BigDecimal("20")); coupon.setStatus(0);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(coupons.listMemberCoupons(9L)).thenReturn(List.of(coupon));
        AiChatRequest request = new AiChatRequest(); request.setMessage("我的优惠券有哪些");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class), coupons)
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(), eq("你目前有以下可用优惠券：\n满100减20（减20）"), eq("query_coupon"), any(), any());
    }
}
