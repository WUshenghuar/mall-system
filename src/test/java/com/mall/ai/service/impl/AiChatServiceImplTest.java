package com.mall.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.entity.AiConversation;
import com.mall.ai.mapper.AiConversationMapper;
import com.mall.ai.service.AiGatewayClient;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.service.LogisticsService;
import com.mall.trade.service.TradeOrderService;
import com.mall.trade.service.TradeRefundService;
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
            Consumer<String> consumer = invocation.getArgument(5);
            consumer.accept("{\"type\":\"text\",\"content\":\"你好\"}");
            return null;
        }).when(gateway).stream(anyLong(), anyString(), anyString(), anyString(), any(), any());
        AiChatRequest request = new AiChatRequest();
        request.setMessage("你好");
        List<String> events = new ArrayList<>();

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, logisticsService, refundService)
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

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, mock(LogisticsService.class), mock(TradeRefundService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(), eq("订单T202609071234567890当前状态：待收货，实付金额：19.90。"), any(), any());
    }
}
