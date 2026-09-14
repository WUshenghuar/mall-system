package com.mall.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.entity.AiConversation;
import com.mall.ai.entity.AiAuditLog;
import com.mall.ai.mapper.AiConversationMapper;
import com.mall.ai.mapper.AiAuditLogMapper;
import com.mall.ai.service.AiGatewayClient;
import com.mall.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeLogistics;
import com.mall.trade.service.LogisticsService;
import com.mall.trade.service.TradeOrderService;
import com.mall.trade.service.TradeRefundService;
import com.mall.product.service.StoreCatalogService;
import com.mall.product.entity.Spu;
import com.mall.marketing.entity.MemberCouponVO;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.entity.Activity;
import com.mall.marketing.service.CouponService;
import com.mall.marketing.service.ActivityService;
import com.mall.member.entity.Member;
import com.mall.member.service.MemberService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiChatServiceImplTest {
    @Test
    void recentLoadsOnlyLatestMemberConversation() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiConversation message = new AiConversation();
        message.setSessionId("session-1");
        message.setRole("assistant");
        message.setContent("你好");
        when(mapper.selectLatest(9L, 20)).thenReturn(List.of(message));

        List<AiConversation> result = new AiChatServiceImpl(mapper, mock(AiGatewayClient.class), new ObjectMapper(),
                mock(TradeOrderService.class), mock(LogisticsService.class), mock(TradeRefundService.class),
                mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class)).recent(9L, 20);

        assertThat(result).containsExactly(message);
        verify(mapper).selectLatest(9L, 20);
    }

    @Test
    void replaysCompletedRequestWithoutCallingGateway() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        AiConversation previous = new AiConversation();
        previous.setContent("已完成的回答");
        when(mapper.selectAssistantByRequest(9L, "session-1", "req-1")).thenReturn(previous);
        AiChatRequest request = newRequest("重复请求");
        request.setRequestId("req-1");
        List<String> events = new ArrayList<>();

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class),
                mock(CouponService.class), mock(MemberService.class)).stream(9L, "session-1", request, events::add);

        assertThat(events).hasSize(2);
        assertThat(events.get(0)).contains("已完成的回答");
        assertThat(events.get(1)).contains("done");
        verifyNoInteractions(gateway);
    }

    @Test
    void returnsInProgressWhenSameRequestIsAlreadyBeingHandled() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        when(mapper.selectAssistantByRequest(9L, "session-1", "req-2")).thenReturn(null);
        when(mapper.insertUserIfAbsent("session-1", "req-2", 9L, "重复请求")).thenReturn(0);
        AiChatRequest request = newRequest("重复请求");
        request.setRequestId("req-2");
        List<String> events = new ArrayList<>();

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class),
                mock(CouponService.class), mock(MemberService.class)).stream(9L, "session-1", request, events::add);

        assertThat(events).containsExactly("{\"type\":\"error\",\"message\":\"客服请求正在处理中，请稍后重试\"}");
        verifyNoInteractions(gateway);
    }

    @Test
    void reclaimsStaleRequestPlaceholderForAThreadThatCanRetry() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        when(mapper.selectAssistantByRequest(9L, "session-1", "req-stale")).thenReturn(null);
        when(mapper.insertUserIfAbsent("session-1", "req-stale", 9L, "超时重试")).thenReturn(0);
        when(mapper.reclaimStaleUserRequest(9L, "session-1", "req-stale")).thenReturn(1);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(6);
            consumer.accept("{\"type\":\"text\",\"content\":\"已恢复处理\"}");
            return null;
        }).when(gateway).stream(anyLong(), anyString(), anyString(), anyString(), anyString(), any(), any());
        AiChatRequest request = newRequest("超时重试"); request.setRequestId("req-stale");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class), mock(LogisticsService.class),
                mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(mapper).reclaimStaleUserRequest(9L, "session-1", "req-stale");
        verify(gateway).stream(anyLong(), anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void releasesRequestPlaceholderWhenGatewayFails() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        when(mapper.selectAssistantByRequest(9L, "session-1", "req-fail")).thenReturn(null);
        when(mapper.insertUserIfAbsent("session-1", "req-fail", 9L, "失败重试")).thenReturn(1);
        doThrow(new BusinessException("客服服务暂不可用")).when(gateway).stream(anyLong(), anyString(), anyString(),
                anyString(), anyString(), any(), any());
        AiChatRequest request = newRequest("失败重试");
        request.setRequestId("req-fail");

        assertThatThrownBy(() -> new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class),
                mock(CouponService.class), mock(MemberService.class)).stream(9L, "session-1", request, ignored -> { }))
                .isInstanceOf(BusinessException.class);

        verify(mapper).discardUserRequest(9L, "session-1", "req-fail");
    }

    @Test
    void releasesRequestPlaceholderWhenGatewayEmitsErrorEvent() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        when(mapper.selectAssistantByRequest(9L, "session-1", "req-event-fail")).thenReturn(null);
        when(mapper.insertUserIfAbsent("session-1", "req-event-fail", 9L, "事件失败")).thenReturn(1);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(6);
            consumer.accept("{\"type\":\"error\",\"message\":\"客服服务暂不可用\"}");
            return null;
        }).when(gateway).stream(anyLong(), anyString(), anyString(), anyString(), anyString(), any(), any());
        AiChatRequest request = newRequest("事件失败");
        request.setRequestId("req-event-fail");

        assertThatThrownBy(() -> new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class),
                mock(CouponService.class), mock(MemberService.class)).stream(9L, "session-1", request, ignored -> { }))
                .isInstanceOf(BusinessException.class);

        verify(mapper).discardUserRequest(9L, "session-1", "req-event-fail");
        verify(mapper, org.mockito.Mockito.never()).insert(any(AiConversation.class));
    }

    @Test
    void releasesRequestPlaceholderWhenBusinessContextFails() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        MemberService memberService = mock(MemberService.class);
        when(mapper.selectAssistantByRequest(9L, "session-1", "req-context-fail")).thenReturn(null);
        when(mapper.insertUserIfAbsent("session-1", "req-context-fail", 9L, "我的会员")).thenReturn(1);
        doThrow(new BusinessException("会员服务暂不可用")).when(memberService).getById(9L);
        AiChatRequest request = newRequest("我的会员");
        request.setRequestId("req-context-fail");

        assertThatThrownBy(() -> new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class),
                mock(CouponService.class), memberService).stream(9L, "session-1", request, ignored -> { }))
                .isInstanceOf(BusinessException.class);

        verify(mapper).discardUserRequest(9L, "session-1", "req-context-fail");
        verifyNoInteractions(gateway);
    }

    @Test
    void recordsFeedbackOnlyForOwnedAssistantMessage() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        when(mapper.updateFeedback(7L, 9L, 1)).thenReturn(1);

        new AiChatServiceImpl(mapper, mock(AiGatewayClient.class), new ObjectMapper(),
                mock(TradeOrderService.class), mock(LogisticsService.class), mock(TradeRefundService.class),
                mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class))
                .feedback(9L, 7L, 1);

        verify(mapper).updateFeedback(7L, 9L, 1);
    }

    @Test
    void recordsChatLifecycleAuditWithoutMessageContent() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        AiAuditLogMapper audit = mock(AiAuditLogMapper.class);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(6);
            consumer.accept("{\"type\":\"text\",\"content\":\"你好\"}");
            return null;
        }).when(gateway).stream(anyLong(), anyString(), anyString(), anyString(), anyString(), any(), any());
        AiChatServiceImpl service = new AiChatServiceImpl(mapper, gateway, new ObjectMapper(),
                mock(TradeOrderService.class), mock(LogisticsService.class), mock(TradeRefundService.class),
                mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class));
        service.setAuditLogMapper(audit);

        service.stream(9L, "session-1", newRequest("hello"), ignored -> { });

        verify(audit, org.mockito.Mockito.times(2)).insert(eq(9L), eq("session-1"),
                org.mockito.ArgumentMatchers.isNull(), eq("chat"), org.mockito.ArgumentMatchers.isNull(),
                anyString(), anyInt(), anyString());
    }

    @Test
    void pagesAuditLogsForOperations() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiAuditLogMapper audit = mock(AiAuditLogMapper.class);
        AiAuditLog record = new AiAuditLog(); record.setEventType("chat"); record.setOutcome("completed");
        when(audit.selectPage(10, 10, "chat", "completed")).thenReturn(List.of(record));
        when(audit.count("chat", "completed")).thenReturn(1L);
        AiChatServiceImpl service = new AiChatServiceImpl(mapper, mock(AiGatewayClient.class), new ObjectMapper(),
                mock(TradeOrderService.class), mock(LogisticsService.class), mock(TradeRefundService.class),
                mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class));
        service.setAuditLogMapper(audit);

        var result = service.auditPage(2, 10, "chat", "completed");

        assertThat(result.getCurrent()).isEqualTo(2);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords()).containsExactly(record);
    }

    @Test
    void exposesAiRuntimeStatusToOperations() {
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        Map<String, Object> status = Map.of("status", "ok", "modelConfigured", false, "embeddingConfigured", false);
        when(gateway.runtimeStatus()).thenReturn(status);
        AiChatServiceImpl service = new AiChatServiceImpl(mock(AiConversationMapper.class), gateway, new ObjectMapper(),
                mock(TradeOrderService.class), mock(LogisticsService.class), mock(TradeRefundService.class),
                mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class));

        assertThat(service.runtimeStatus()).isEqualTo(status);
    }

    @Test
    void exposesAiRuntimeMetricsToOperations() {
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        Map<String, Object> metrics = Map.of("requests", 4, "sla", Map.of("status", "healthy"));
        when(gateway.runtimeMetrics()).thenReturn(metrics);
        AiChatServiceImpl service = new AiChatServiceImpl(mock(AiConversationMapper.class), gateway, new ObjectMapper(),
                mock(TradeOrderService.class), mock(LogisticsService.class), mock(TradeRefundService.class),
                mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class));

        assertThat(service.runtimeMetrics()).isEqualTo(metrics);
    }

    @Test
    void exposesFeedbackStatsForOperations() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        when(mapper.selectFeedbackStats()).thenReturn(Map.of("total", 4L, "positive", 3L, "negative", 1L));

        Map<String, Object> result = new AiChatServiceImpl(mapper, mock(AiGatewayClient.class), new ObjectMapper(),
                mock(TradeOrderService.class), mock(LogisticsService.class), mock(TradeRefundService.class),
                mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class)).feedbackStats();

        assertThat(result).containsEntry("positive", 3L).containsEntry("negative", 1L);
    }

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

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, logisticsService, refundService, mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class))
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

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(), eq("订单T202609071234567890当前状态：待收货，实付金额：19.90。"), eq("query_order"), any(), any());
    }

    @Test
    void streamRoutesPackageQuestionToLogisticsTool() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        TradeOrderService orderService = mock(TradeOrderService.class);
        LogisticsService logisticsService = mock(LogisticsService.class);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        TradeOrder order = new TradeOrder(); order.setOrderStatus(2); order.setPayAmount(new java.math.BigDecimal("19.90"));
        when(orderService.getOwnedByOrderNo("T202609071234567890", 9L)).thenReturn(order);
        TradeLogistics logistics = new TradeLogistics(); logistics.setLogisticsCompany("DHL"); logistics.setLogisticsNo("DHL-001");
        when(logisticsService.getByOrderNo("T202609071234567890")).thenReturn(logistics);
        AiChatRequest request = new AiChatRequest(); request.setMessage("我的包裹到哪了 T202609071234567890");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, logisticsService,
                mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("订单T202609071234567890的物流：DHL，运单号：DHL-001。"), eq("query_logistics"), any(), any());
    }

    @Test
    void streamExecutesModelPlannedReadOnlyToolAfterOwnershipCheck() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        TradeOrderService orderService = mock(TradeOrderService.class);
        LogisticsService logisticsService = mock(LogisticsService.class);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(gateway.plan(anyLong(), anyString(), anyString(), any())).thenReturn(Map.of(
                "tool", "query_logistics",
                "arguments", Map.of("order_no", " T202609071234567890 ", "unexpected", "ignore-me")));
        TradeOrder order = new TradeOrder(); order.setOrderStatus(2); order.setPayAmount(new java.math.BigDecimal("19.90"));
        when(orderService.getOwnedByOrderNo("T202609071234567890", 9L)).thenReturn(order);
        TradeLogistics logistics = new TradeLogistics(); logistics.setLogisticsCompany("DHL"); logistics.setLogisticsNo("DHL-001");
        when(logisticsService.getByOrderNo("T202609071234567890")).thenReturn(logistics);
        AiChatRequest request = new AiChatRequest(); request.setMessage("帮我查这笔订单");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, logisticsService,
                mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).plan(anyLong(), anyString(), anyString(), any());
        ArgumentCaptor<List> toolResults = ArgumentCaptor.forClass(List.class);
        verify(gateway).plan(anyLong(), anyString(), anyString(), any(), toolResults.capture());
        Map<?, ?> plannedResult = (Map<?, ?>) toolResults.getValue().get(0);
        Map<?, ?> plannedArguments = (Map<?, ?>) plannedResult.get("arguments");
        assertThat(plannedArguments.get("order_no")).isEqualTo("T202609071234567890");
        assertThat(plannedArguments.containsKey("unexpected")).isFalse();
        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("订单T202609071234567890的物流：DHL，运单号：DHL-001。"), eq("query_logistics"), any(), any());
    }

    @Test
    void executesMultipleModelPlannedReadOnlyToolsAndCombinesContext() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        StoreCatalogService catalog = mock(StoreCatalogService.class);
        MemberService members = mock(MemberService.class);
        IPage page = mock(IPage.class);
        Spu product = new Spu(); product.setSpuName("跨境耳机");
        Member member = new Member(); member.setLevel(1); member.setPoints(88); member.setTotalAmount(new java.math.BigDecimal("1200.00"));
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(gateway.plan(anyLong(), anyString(), anyString(), any())).thenReturn(Map.of("tools", List.of(
                Map.of("tool", "query_member", "arguments", Map.of()),
                Map.of("tool", "query_product", "arguments", Map.of("keyword", "耳机")))));
        when(members.getById(9L)).thenReturn(member);
        when(catalog.products(1, 3, null, "耳机")).thenReturn(page);
        when(page.getRecords()).thenReturn(List.of(product));
        AiChatRequest request = new AiChatRequest(); request.setMessage("耳机和账户权益");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class), mock(LogisticsService.class),
                mock(TradeRefundService.class), catalog, mock(CouponService.class), members)
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("你的会员等级：Gold 会员，积分：88，累计消费：1200.00。\n为你找到的上架商品：\n跨境耳机"),
                eq("query_member,query_product"), any(), any());
    }

    @Test
    void stopsPlanningAfterThreeAcceptedTools() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        StoreCatalogService catalog = mock(StoreCatalogService.class);
        MemberService members = mock(MemberService.class);
        ActivityService activities = mock(ActivityService.class);
        IPage page = mock(IPage.class);
        Spu product = new Spu(); product.setSpuName("跨境耳机");
        Member member = new Member(); member.setLevel(1); member.setPoints(88); member.setTotalAmount(new java.math.BigDecimal("1200.00"));
        Activity activity = new Activity(); activity.setActivityName("秋季好物周");
        activity.setEndTime(LocalDateTime.of(2026, 9, 30, 23, 59));
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(gateway.plan(anyLong(), anyString(), anyString(), any())).thenReturn(Map.of("tools", List.of(
                Map.of("tool", "query_member", "arguments", Map.of()),
                Map.of("tool", "query_product", "arguments", Map.of("keyword", "耳机")),
                Map.of("tool", "query_activity", "arguments", Map.of()))));
        when(members.getById(9L)).thenReturn(member);
        when(catalog.products(1, 3, null, "耳机")).thenReturn(page);
        when(page.getRecords()).thenReturn(List.of(product));
        when(activities.selectActive()).thenReturn(List.of(activity));

        AiChatServiceImpl service = new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), catalog, mock(CouponService.class), members);
        service.setActivityService(activities);
        service.stream(9L, "session-1", newRequest("我的会员、耳机商品和活动"), ignored -> { });

        verify(gateway, org.mockito.Mockito.never()).plan(anyLong(), anyString(), anyString(), any(), anyList());
        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("你的会员等级：Gold 会员，积分：88，累计消费：1200.00。\n为你找到的上架商品：\n跨境耳机\n当前进行中的活动：\n秋季好物周（截止 2026-09-30T23:59）"),
                eq("query_member,query_product,query_activity"), any(), any());
    }

    @Test
    void plansAdditionalToolsAfterAConfiguredRuleContextForMultipleTopics() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        TradeOrderService orders = mock(TradeOrderService.class);
        LogisticsService logistics = mock(LogisticsService.class);
        ActivityService activities = mock(ActivityService.class);
        IPage<TradeOrder> page = mock(IPage.class);
        TradeOrder order = new TradeOrder(); order.setOrderNo("T202609071234567890"); order.setOrderStatus(2);
        TradeLogistics tracking = new TradeLogistics(); tracking.setLogisticsCompany("DHL"); tracking.setLogisticsNo("DHL-001");
        Activity activity = new Activity(); activity.setActivityName("秋季好物周"); activity.setActivityType("DISCOUNT");
        activity.setEndTime(LocalDateTime.of(2026, 9, 30, 23, 59));
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(orders.selectPage(1, 3, 9L, null)).thenReturn(page);
        when(page.getRecords()).thenReturn(List.of(order));
        when(logistics.getByOrderNo("T202609071234567890")).thenReturn(tracking);
        when(activities.selectActive()).thenReturn(List.of(activity));
        when(gateway.plan(anyLong(), anyString(), anyString(), any())).thenReturn(
                Map.of("tool", "query_activity", "arguments", Map.of()));
        when(gateway.plan(anyLong(), anyString(), anyString(), any(), anyList())).thenReturn(Map.of());

        AiChatServiceImpl service = new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orders, logistics,
                mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class));
        service.setActivityService(activities);
        service.stream(9L, "session-1", newRequest("my package and promotion"), ignored -> { });

        verify(gateway).plan(anyLong(), anyString(), anyString(), any());
        verify(gateway).plan(anyLong(), anyString(), anyString(), any(), anyList());
        ArgumentCaptor<List> toolResults = ArgumentCaptor.forClass(List.class);
        verify(gateway).plan(anyLong(), anyString(), anyString(), any(), toolResults.capture());
        Map<?, ?> firstToolResult = (Map<?, ?>) toolResults.getValue().get(0);
        assertThat(firstToolResult.get("callId")).isEqualTo("call-1");
        assertThat(firstToolResult.get("tool")).isEqualTo("query_logistics");
        assertThat(firstToolResult.containsKey("arguments")).isTrue();
        assertThat(firstToolResult.containsKey("content")).isTrue();
        verify(gateway).stream(anyLong(), anyString(), anyString(),
                argThat(value -> value.contains("DHL-001") && value.contains("秋季好物周")),
                eq("query_logistics,query_activity"), any(), any());
    }

    @Test
    void performsOneFollowUpPlanForASecondBusinessTopic() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        ActivityService activities = mock(ActivityService.class);
        MemberService members = mock(MemberService.class);
        Activity activity = new Activity();
        activity.setActivityName("秋季好物周"); activity.setActivityType("DISCOUNT");
        activity.setEndTime(LocalDateTime.of(2026, 9, 30, 23, 59));
        Member member = new Member(); member.setLevel(1); member.setPoints(88); member.setTotalAmount(new java.math.BigDecimal("1200.00"));
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(gateway.plan(anyLong(), anyString(), anyString(), any())).thenReturn(Map.of("tool", "query_activity", "arguments", Map.of()));
        when(gateway.plan(anyLong(), anyString(), anyString(), any(), anyList())).thenReturn(
                Map.of("tool", "query_member", "arguments", Map.of()), Map.of());
        when(activities.selectActive()).thenReturn(List.of(activity));
        when(members.getById(9L)).thenReturn(member);

        AiChatServiceImpl service = new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class), members);
        service.setActivityService(activities);
        service.stream(9L, "session-1", newRequest("物流和活动"), ignored -> { });

        verify(gateway).plan(anyLong(), anyString(), anyString(), any());
        verify(gateway, times(2)).plan(anyLong(), anyString(), anyString(), any(), anyList());
        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("当前进行中的活动：\n秋季好物周（DISCOUNT，截止 2026-09-30T23:59）\n你的会员等级：Gold 会员，积分：88，累计消费：1200.00。"),
                eq("query_activity,query_member"), any(), any());
    }

    @Test
    void performsFollowUpPlanWhenTheMessageHasOnlyOneKeywordTopic() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        ActivityService activities = mock(ActivityService.class);
        MemberService members = mock(MemberService.class);
        Activity activity = new Activity();
        activity.setActivityName("秋季好物周"); activity.setActivityType("DISCOUNT");
        activity.setEndTime(LocalDateTime.of(2026, 9, 30, 23, 59));
        Member member = new Member(); member.setLevel(1); member.setPoints(88); member.setTotalAmount(new java.math.BigDecimal("1200.00"));
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(gateway.plan(anyLong(), anyString(), anyString(), any())).thenReturn(Map.of("tool", "query_activity", "arguments", Map.of()));
        when(gateway.plan(anyLong(), anyString(), anyString(), any(), anyList())).thenReturn(
                Map.of("tool", "query_member", "arguments", Map.of()), Map.of());
        when(activities.selectActive()).thenReturn(List.of(activity));
        when(members.getById(9L)).thenReturn(member);

        AiChatServiceImpl service = new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class), members);
        service.setActivityService(activities);
        service.stream(9L, "session-1", newRequest("退款"), ignored -> { });

        verify(gateway, times(2)).plan(anyLong(), anyString(), anyString(), any(), anyList());
        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("当前进行中的活动：\n秋季好物周（DISCOUNT，截止 2026-09-30T23:59）\n你的会员等级：Gold 会员，积分：88，累计消费：1200.00。"),
                eq("query_activity,query_member"), any(), any());
    }

    @Test
    void performsAtMostThreePlansForThreeBusinessTopics() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        ActivityService activities = mock(ActivityService.class);
        StoreCatalogService catalog = mock(StoreCatalogService.class);
        MemberService members = mock(MemberService.class);
        IPage page = mock(IPage.class);
        Activity activity = new Activity(); activity.setActivityName("秋季好物周"); activity.setActivityType("DISCOUNT");
        activity.setEndTime(LocalDateTime.of(2026, 9, 30, 23, 59));
        Spu product = new Spu(); product.setSpuName("跨境耳机");
        Member member = new Member(); member.setLevel(1); member.setPoints(88); member.setTotalAmount(new java.math.BigDecimal("1200.00"));
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(gateway.plan(anyLong(), anyString(), anyString(), any())).thenReturn(Map.of("tool", "query_activity", "arguments", Map.of()));
        when(gateway.plan(anyLong(), anyString(), anyString(), any(), anyList())).thenReturn(
                Map.of("tool", "query_product", "arguments", Map.of("keyword", "耳机")),
                Map.of("tool", "query_member", "arguments", Map.of()));
        when(activities.selectActive()).thenReturn(List.of(activity));
        when(catalog.products(1, 3, null, "耳机")).thenReturn(page);
        when(page.getRecords()).thenReturn(List.of(product));
        when(members.getById(9L)).thenReturn(member);

        AiChatServiceImpl service = new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), catalog, mock(CouponService.class), members);
        service.setActivityService(activities);
        service.stream(9L, "session-1", newRequest("物流、活动和耳机商品"), ignored -> { });

        verify(gateway).plan(anyLong(), anyString(), anyString(), any());
        verify(gateway, times(2)).plan(anyLong(), anyString(), anyString(), any(), anyList());
        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("当前进行中的活动：\n秋季好物周（DISCOUNT，截止 2026-09-30T23:59）\n为你找到的上架商品：\n跨境耳机\n你的会员等级：Gold 会员，积分：88，累计消费：1200.00。"),
                eq("query_activity,query_product,query_member"), any(), any());
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

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(), eq("你最近的订单：\n订单T202609071234567890：待发货，实付金额：19.90"), eq("query_order"), any(), any());
    }

    @Test
    void streamQueriesRecentOwnedLogisticsForEnglishMessage() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        TradeOrderService orderService = mock(TradeOrderService.class);
        LogisticsService logisticsService = mock(LogisticsService.class);
        IPage<TradeOrder> page = mock(IPage.class);
        TradeOrder order = new TradeOrder(); order.setOrderNo("T202609071234567890"); order.setOrderStatus(2);
        order.setPayAmount(new java.math.BigDecimal("19.90"));
        TradeLogistics logistics = new TradeLogistics(); logistics.setLogisticsCompany("DHL"); logistics.setLogisticsNo("DHL-001");
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(orderService.selectPage(1, 3, 9L, null)).thenReturn(page);
        when(page.getRecords()).thenReturn(List.of(order));
        when(logisticsService.getByOrderNo("T202609071234567890")).thenReturn(logistics);
        AiChatRequest request = new AiChatRequest(); request.setMessage("Where is my package?");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, logisticsService,
                mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("Your latest order T202609071234567890 logistics: DHL, tracking number: DHL-001."), eq("query_logistics"), any(), any());
    }

    @Test
    void streamLeavesEnglishCouponPolicyForKnowledgeRetrieval() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        TradeOrderService orderService = mock(TradeOrderService.class);
        CouponService couponService = mock(CouponService.class);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        AiChatRequest request = new AiChatRequest(); request.setMessage("How do coupons work?");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orderService, mock(LogisticsService.class),
                mock(TradeRefundService.class), mock(StoreCatalogService.class), couponService, mock(MemberService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(), eq(""), eq(""), any(), any());
        verifyNoInteractions(orderService, couponService);
    }

    @Test
    void streamQueriesActiveActivitiesAsReadOnlyContext() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        ActivityService activities = mock(ActivityService.class);
        Activity activity = new Activity(); activity.setActivityName("秋季折扣");
        activity.setEndTime(java.time.LocalDateTime.of(2026, 9, 30, 23, 59));
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(activities.selectActive()).thenReturn(List.of(activity));
        AiChatServiceImpl service = new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class),
                mock(CouponService.class), mock(MemberService.class));
        service.setActivityService(activities);

        service.stream(9L, "session-1", newRequest("现在有什么活动"), ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("当前进行中的活动：\n秋季折扣（截止 2026-09-30T23:59）"), eq("query_activity"), any(), any());
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
                mock(LogisticsService.class), mock(TradeRefundService.class), catalog, mock(CouponService.class), mock(MemberService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(), eq("为你找到的上架商品：\n跨境耳机"), eq("query_product"), any(), any());
    }

    @Test
    void streamIncludesPublishedProductPriceWhenCatalogProvidesIt() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        StoreCatalogService catalog = mock(StoreCatalogService.class);
        IPage page = mock(IPage.class);
        Spu product = new Spu(); product.setSpuName("跨境耳机"); product.setMinPrice(new java.math.BigDecimal("29.90")); product.setCurrency("USD");
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(catalog.products(1, 3, null, "耳机")).thenReturn(page);
        when(page.getRecords()).thenReturn(List.of(product));

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class), mock(LogisticsService.class),
                mock(TradeRefundService.class), catalog, mock(CouponService.class), mock(MemberService.class))
                .stream(9L, "session-1", newRequest("查询耳机商品"), ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("为你找到的上架商品：\n跨境耳机（起 USD 29.90）"), eq("query_product"), any(), any());
    }

    @Test
    void streamCleansEnglishProductSearchKeywords() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        StoreCatalogService catalog = mock(StoreCatalogService.class);
        IPage page = mock(IPage.class);
        Spu product = new Spu(); product.setSpuName("Red Dress"); product.setCurrency("USD");
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(catalog.products(1, 3, null, "red dress")).thenReturn(page);
        when(page.getRecords()).thenReturn(List.of(product));

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class), mock(LogisticsService.class),
                mock(TradeRefundService.class), catalog, mock(CouponService.class), mock(MemberService.class))
                .stream(9L, "session-1", newRequest("Find red dress products"), ignored -> { });

        verify(catalog).products(1, 3, null, "red dress");
        verify(gateway).stream(anyLong(), anyString(), anyString(), eq("Matching products:\nRed Dress"), eq("query_product"), any(), any());
    }

    @Test
    void streamLeavesRefundRulesForKnowledgeRetrieval() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        AiChatRequest request = new AiChatRequest(); request.setMessage("退款规则是什么");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class))
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
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class), coupons, mock(MemberService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(), eq("你目前有以下可用优惠券：\n满100减20（减20）"), eq("query_coupon"), any(), any());
    }

    @Test
    void streamQueriesClaimablePlatformCouponsSeparatelyFromMemberCoupons() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        CouponService coupons = mock(CouponService.class);
        Coupon coupon = new Coupon(); coupon.setCouponName("新客券"); coupon.setDiscount(new java.math.BigDecimal("20"));
        coupon.setValidEnd(java.time.LocalDateTime.of(2026, 9, 30, 23, 59));
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(coupons.listAvailable()).thenReturn(List.of(coupon));
        AiChatRequest request = new AiChatRequest(); request.setMessage("现在有哪些优惠券");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class), mock(LogisticsService.class),
                mock(TradeRefundService.class), mock(StoreCatalogService.class), coupons, mock(MemberService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(coupons).listAvailable();
        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("当前可领取的优惠券：\n新客券（优惠 20，截止 2026-09-30T23:59）"), eq("query_coupon"), any(), any());
    }

    @Test
    void streamChecksReturnEligibilityUsingTheSameOrderRulesAsRefundApply() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        TradeOrderService orders = mock(TradeOrderService.class);
        TradeRefundService refunds = mock(TradeRefundService.class);
        TradeOrder order = new TradeOrder(); order.setOrderStatus(2);
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(orders.getOwnedByOrderNo("T202609071234567890", 9L)).thenReturn(order);
        when(refunds.selectMemberPage(9L, 1, 100)).thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());
        AiChatRequest request = new AiChatRequest(); request.setMessage("这个订单能退吗 T202609071234567890");

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), orders, mock(LogisticsService.class), refunds,
                mock(StoreCatalogService.class), mock(CouponService.class), mock(MemberService.class))
                .stream(9L, "session-1", request, ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("订单T202609071234567890可申请退货退款。"), eq("query_return_eligibility"), any(), any());
    }

    @Test
    void streamQueriesOwnedMemberLevelAndPoints() {
        AiConversationMapper mapper = mock(AiConversationMapper.class);
        AiGatewayClient gateway = mock(AiGatewayClient.class);
        MemberService memberService = mock(MemberService.class);
        Member member = new Member(); member.setLevel(1); member.setPoints(88); member.setTotalAmount(new java.math.BigDecimal("1200.00"));
        when(mapper.selectRecent(anyLong(), anyString(), anyInt())).thenReturn(List.of());
        when(memberService.getById(9L)).thenReturn(member);
        when(memberService.listPointsLogs(9L)).thenReturn(List.of());

        new AiChatServiceImpl(mapper, gateway, new ObjectMapper(), mock(TradeOrderService.class),
                mock(LogisticsService.class), mock(TradeRefundService.class), mock(StoreCatalogService.class),
                mock(CouponService.class), memberService)
                .stream(9L, "session-1", newRequest("我的会员等级和积分"), ignored -> { });

        verify(gateway).stream(anyLong(), anyString(), anyString(),
                eq("你的会员等级：Gold 会员，积分：88，累计消费：1200.00。"), eq("query_member"), any(), any());
    }

    private AiChatRequest newRequest(String message) {
        AiChatRequest request = new AiChatRequest(); request.setMessage(message); return request;
    }
}
