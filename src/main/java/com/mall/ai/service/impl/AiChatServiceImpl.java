package com.mall.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.entity.AiAuditLog;
import com.mall.ai.entity.AiConversation;
import com.mall.ai.mapper.AiAuditLogMapper;
import com.mall.ai.mapper.AiConversationMapper;
import com.mall.ai.service.AiChatService;
import com.mall.ai.service.AiGatewayClient;
import com.mall.common.exception.BusinessException;
import com.mall.trade.entity.TradeLogistics;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeRefund;
import com.mall.trade.service.LogisticsService;
import com.mall.trade.service.TradeOrderService;
import com.mall.trade.service.TradeRefundService;
import com.mall.product.entity.Spu;
import com.mall.product.service.StoreCatalogService;
import com.mall.marketing.entity.MemberCouponVO;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.entity.Activity;
import com.mall.marketing.service.CouponService;
import com.mall.marketing.service.ActivityService;
import com.mall.member.entity.Member;
import com.mall.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatServiceImpl implements AiChatService {
    private static final Pattern ORDER_NO = Pattern.compile("T\\d{18}");
    private final AiConversationMapper conversationMapper;
    private final AiGatewayClient gatewayClient;
    private final ObjectMapper objectMapper;
    private final TradeOrderService tradeOrderService;
    private final LogisticsService logisticsService;
    private final TradeRefundService tradeRefundService;
    private final StoreCatalogService storeCatalogService;
    private final CouponService couponService;
    private final MemberService memberService;
    @Autowired(required = false)
    private AiAuditLogMapper auditLogMapper;
    @Autowired(required = false)
    private ActivityService activityService;

    @Override
    public void stream(Long memberId, String conversationId, AiChatRequest request, Consumer<String> eventConsumer) {
        String requestId = StringUtils.hasText(request.getRequestId()) ? request.getRequestId().trim() : null;
        if (requestId != null) {
            AiConversation previous = conversationMapper.selectAssistantByRequest(memberId, conversationId, requestId);
            if (previous != null) {
                recordAudit(memberId, conversationId, requestId, "chat", null, "replayed", 0, "idempotent_replay");
                replay(previous, eventConsumer);
                return;
            }
            if (conversationMapper.insertUserIfAbsent(conversationId, requestId, memberId, request.getMessage()) != 1) {
                previous = conversationMapper.selectAssistantByRequest(memberId, conversationId, requestId);
                if (previous != null) replay(previous, eventConsumer);
                else if (conversationMapper.reclaimStaleUserRequest(memberId, conversationId, requestId) != 1) {
                    recordAudit(memberId, conversationId, requestId, "chat", null, "in_progress", 0, "duplicate_request");
                    eventConsumer.accept("{\"type\":\"error\",\"message\":\"客服请求正在处理中，请稍后重试\"}");
                    return;
                }
            }
        } else {
            save(memberId, conversationId, null, "user", request.getMessage(), 0, 0);
        }
        recordAudit(memberId, conversationId, requestId, "chat", null, "started", 0, "request_received");
        StringBuilder answer = new StringBuilder();
        AtomicReference<String> failure = new AtomicReference<>();
        String selectedTool = "";
        long start = System.currentTimeMillis();
        try {
            List<AiConversation> history = conversationMapper.selectRecent(memberId, conversationId, 10);
            start = System.currentTimeMillis();
            String businessContext = businessContext(memberId, request.getMessage());
            selectedTool = businessTool(request.getMessage(), businessContext);
            if (businessContext.isBlank() || hasMultipleBusinessTopics(request.getMessage())) {
                Map<String, Object> decision = gatewayClient.plan(memberId, conversationId, request.getMessage(), history);
                List<String> contextParts = new ArrayList<>();
                List<String> toolNames = new ArrayList<>();
                if (!businessContext.isBlank()) {
                    contextParts.add(businessContext);
                    if (!selectedTool.isBlank()) toolNames.add(selectedTool);
                }
                int added = appendPlannedContexts(memberId, conversationId, requestId, request.getMessage(), decision, contextParts, toolNames, start);
                for (int round = 1; added > 0 && toolNames.size() < 3 && round < 3; round++) {
                    List<String> toolResults = new ArrayList<>();
                    for (int index = 0; index < contextParts.size(); index++) {
                        toolResults.add(toolNames.get(index) + ": " + contextParts.get(index));
                    }
                    Map<String, Object> followUp = gatewayClient.plan(memberId, conversationId, request.getMessage(), history, toolResults);
                    added = appendPlannedContexts(memberId, conversationId, requestId, request.getMessage(), followUp, contextParts, toolNames, start);
                }
                if (!contextParts.isEmpty()) {
                    businessContext = String.join("\n", contextParts);
                    selectedTool = String.join(",", toolNames.stream().distinct().toList());
                }
            }
            if (!selectedTool.isBlank() && !businessContext.isBlank()) {
                for (String tool : selectedTool.split(",")) {
                    recordAudit(memberId, conversationId, requestId, "tool", tool, "executed",
                            (int) (System.currentTimeMillis() - start), "read_only");
                }
            }
            gatewayClient.stream(memberId, conversationId, request.getMessage(), businessContext,
                    selectedTool, history, event -> {
                String error = readError(event);
                if (error != null) {
                    failure.set(error);
                    return;
                }
                answer.append(readText(event));
                eventConsumer.accept(event);
            });
            if (failure.get() != null) throw new BusinessException(failure.get());
            save(memberId, conversationId, requestId, "assistant", answer.toString(), 0, (int) (System.currentTimeMillis() - start));
            recordAudit(memberId, conversationId, requestId, "chat", selectedTool.isBlank() ? null : selectedTool,
                    "completed", (int) (System.currentTimeMillis() - start), "stream_completed");
        } catch (RuntimeException e) {
            recordAudit(memberId, conversationId, requestId, "chat", selectedTool.isBlank() ? null : selectedTool,
                    "failed", (int) (System.currentTimeMillis() - start), "safe_failure");
            if (requestId != null) conversationMapper.discardUserRequest(memberId, conversationId, requestId);
            throw e;
        }
    }

    @Override
    public List<AiConversation> recent(Long memberId, int limit) {
        return conversationMapper.selectLatest(memberId, Math.min(Math.max(limit, 1), 20));
    }

    @Override
    public void feedback(Long memberId, Long messageId, Integer feedback) {
        if (feedback == null || (feedback != 1 && feedback != -1)) {
            throw new BusinessException("反馈值只能是有帮助或没帮助");
        }
        if (conversationMapper.updateFeedback(messageId, memberId, feedback) != 1) {
            throw new BusinessException("只能评价自己的 AI 回复");
        }
    }

    @Override
    public Map<String, Object> feedbackStats() {
        return conversationMapper.selectFeedbackStats();
    }

    @Override
    public Map<String, Object> runtimeStatus() {
        return gatewayClient.runtimeStatus();
    }

    @Override
    public IPage<AiAuditLog> auditPage(Integer page, Integer size, String eventType, String outcome) {
        int current = Math.min(Math.max(page == null ? 1 : page, 1), 10_000);
        int pageSize = Math.min(Math.max(size == null ? 20 : size, 1), 100);
        String selectedEvent = StringUtils.hasText(eventType) ? eventType.trim() : null;
        String selectedOutcome = StringUtils.hasText(outcome) ? outcome.trim() : null;
        Page<AiAuditLog> result = new Page<>(current, pageSize);
        if (auditLogMapper == null) return result;
        result.setRecords(auditLogMapper.selectPage((current - 1) * pageSize, pageSize, selectedEvent, selectedOutcome));
        result.setTotal(auditLogMapper.count(selectedEvent, selectedOutcome));
        return result;
    }

    private String businessTool(String message, String context) {
        if (context.isBlank()) return "";
        return intent(message);
    }

    private String intent(String message) {
        if (containsAny(message, "物流", "快递", "运单", "包裹", "配送", "追踪", "轨迹", "tracking", "track", "delivery", "shipment", "package")) return "query_logistics";
        if (containsAny(message, "能退吗", "能不能退", "退款资格", "退货资格", "是否可退", "return eligibility", "can i return", "eligible for a refund")) return "query_return_eligibility";
        if (containsAny(message, "退款", "退货", "售后", "退钱", "refund", "return", "money back")) return "query_refund";
        if (containsAny(message, "会员", "积分", "等级", "成长", "成长值", "member", "membership", "points", "loyalty")) return "query_member";
        if (containsAny(message, "税费", "关税", "税金", "币种", "tax", "tariff", "duty", "currency")) return "query_tax";
        if (containsAny(message, "活动", "促销", "限时", "秒杀", "activity", "promotion", "campaign", "sale", "deal")) return "query_activity";
        if (containsAny(message, "优惠券", "券包", "折扣券", "coupon", "discount", "promo", "voucher")) return "query_coupon";
        if (containsAny(message, "商品", "产品", "推荐", "找", "款式", "规格", "product", "item", "recommend", "style", "size")) return "query_product";
        return "query_order";
    }

    private boolean containsAny(String message, String... keywords) {
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        return java.util.Arrays.stream(keywords).anyMatch(keyword -> normalized.contains(keyword.toLowerCase(java.util.Locale.ROOT)));
    }

    private String plannedTool(Map<String, Object> decision) {
        Object value = decision == null ? null : decision.get("tool");
        return value instanceof String tool && isSupportedTool(tool) ? tool : "";
    }

    private List<Map<String, Object>> plannedTools(Map<String, Object> decision) {
        List<Map<String, Object>> plans = new ArrayList<>();
        if (decision != null && decision.get("tools") instanceof List<?> values) {
            for (Object value : values) {
                if (value instanceof Map<?, ?> raw) {
                    Map<String, Object> plan = new java.util.HashMap<>();
                    raw.forEach((key, item) -> { if (key instanceof String name) plan.put(name, item); });
                    plans.add(plan);
                }
                if (plans.size() == 3) break;
            }
        }
        if (plans.isEmpty() && decision != null && !plannedTool(decision).isBlank()) plans.add(decision);
        return plans;
    }

    private int appendPlannedContexts(Long memberId, String conversationId, String requestId, String message,
                                      Map<String, Object> decision, List<String> contextParts, List<String> toolNames, long start) {
        int added = 0;
        for (Map<String, Object> plan : plannedTools(decision)) {
            if (toolNames.size() >= 3) break;
            String plannedTool = plannedTool(plan);
            if (plannedTool.isBlank() || toolNames.contains(plannedTool)) continue;
            String plannedContext = businessContext(memberId, message, plannedTool, plannedOrderNo(plan), plannedKeyword(plan));
            if (!plannedContext.isBlank()) {
                contextParts.add(plannedContext);
                toolNames.add(plannedTool);
                added++;
                recordAudit(memberId, conversationId, requestId, "tool_plan", plannedTool, "accepted",
                        (int) (System.currentTimeMillis() - start), "model_read_only_plan");
            }
        }
        return added;
    }

    private boolean hasMultipleBusinessTopics(String message) {
        List<String[]> topics = List.of(
                new String[]{"物流", "快递", "运单", "包裹", "tracking", "track", "delivery", "shipment", "package"},
                new String[]{"退款", "退货", "refund", "return", "money back"},
                new String[]{"商品", "产品", "product", "item", "recommend", "style", "size"},
                new String[]{"优惠券", "coupon", "discount", "promo", "voucher"},
                new String[]{"会员", "积分", "等级", "成长", "member", "membership", "points", "loyalty"},
                new String[]{"活动", "促销", "限时", "秒杀", "activity", "promotion", "campaign", "sale", "deal"},
                new String[]{"税费", "关税", "税金", "币种", "tax", "tariff", "duty", "currency"},
                new String[]{"订单", "order"});
        return topics.stream().filter(topic -> containsAny(message, topic)).limit(2).count() == 2;
    }

    private String plannedOrderNo(Map<String, Object> decision) {
        Object arguments = decision == null ? null : decision.get("arguments");
        if (arguments instanceof Map<?, ?> values && values.get("order_no") instanceof String orderNo) return orderNo;
        return null;
    }

    private String plannedKeyword(Map<String, Object> decision) {
        Object arguments = decision == null ? null : decision.get("arguments");
        if (arguments instanceof Map<?, ?> values && values.get("keyword") instanceof String keyword && StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            return value.substring(0, Math.min(value.length(), 128));
        }
        return null;
    }

    private boolean isSupportedTool(String tool) {
        return switch (tool) {
            case "query_order", "query_logistics", "query_refund", "query_product", "query_coupon", "query_member", "query_tax", "query_activity", "query_return_eligibility" -> true;
            default -> false;
        };
    }

    private String businessContext(Long memberId, String message) {
        return businessContext(memberId, message, intent(message), null, null);
    }

    private String businessContext(Long memberId, String message, String tool, String plannedOrderNo, String plannedKeyword) {
        if ("query_activity".equals(tool)) return recentContext(memberId, message, tool);
        String orderNo = extractOrderNo(message, plannedOrderNo);
        if ("query_return_eligibility".equals(tool) && orderNo == null) {
            return isEnglishMessage(message) ? "Please provide your order number so I can check refund or return eligibility."
                    : "请提供订单号，我才能查询退款或退货资格。";
        }
        if (orderNo == null) return recentContext(memberId, message, tool, plannedKeyword);
        boolean english = isEnglishMessage(message);
        try {
            TradeOrder order = tradeOrderService.getOwnedByOrderNo(orderNo, memberId);
            if ("query_logistics".equals(tool)) {
                TradeLogistics logistics = logisticsService.getByOrderNo(orderNo);
                return logistics == null ? (english ? "No tracking information is available for order " + orderNo + "."
                                : "订单" + orderNo + "暂未录入物流信息。")
                        : (english ? "Order " + orderNo + " logistics: " + logistics.getLogisticsCompany() + ", tracking number: " + logistics.getLogisticsNo() + "."
                                : "订单" + orderNo + "的物流：" + logistics.getLogisticsCompany() + "，运单号：" + logistics.getLogisticsNo() + "。");
            }
            if ("query_refund".equals(tool)) {
                TradeRefund refund = tradeRefundService.selectMemberPage(memberId, 1, 100).getRecords().stream()
                        .filter(item -> orderNo.equals(item.getOrderNo())).findFirst().orElse(null);
                return refund == null ? (english ? "Order " + orderNo + " has no refund request. Order status: " + orderStatusEnglish(order.getOrderStatus()) + "."
                                : "订单" + orderNo + "当前没有退款申请，订单状态：" + orderStatus(order.getOrderStatus()) + "。")
                        : (english ? "Order " + orderNo + " refund status: " + refundStatusEnglish(refund.getRefundStatus()) + "."
                                : "订单" + orderNo + "的退款状态：" + refundStatus(refund.getRefundStatus()) + "。");
            }
            if ("query_return_eligibility".equals(tool)) {
                boolean activeRefund = tradeRefundService.selectMemberPage(memberId, 1, 100).getRecords().stream()
                        .anyMatch(item -> orderNo.equals(item.getOrderNo()) && List.of(0, 1, 3, 4).contains(item.getRefundStatus()));
                if (activeRefund) return english ? "Order " + orderNo + " already has an active after-sales request."
                        : "订单" + orderNo + "已有处理中售后申请，不能重复提交。";
                if (Integer.valueOf(1).equals(order.getOrderStatus())) return english ? "Order " + orderNo + " is eligible for a refund only."
                        : "订单" + orderNo + "可申请仅退款。";
                if (Integer.valueOf(2).equals(order.getOrderStatus()) || Integer.valueOf(3).equals(order.getOrderStatus())) {
                    return english ? "Order " + orderNo + " is eligible for a return and refund."
                            : "订单" + orderNo + "可申请退货退款。";
                }
                return english ? "Order " + orderNo + " is not currently eligible for a refund or return."
                        : "订单" + orderNo + "当前状态不可申请退款或退货。";
            }
            if ("query_tax".equals(tool)) {
                return english ? "Order " + orderNo + " currency: " + (order.getCurrency() == null ? "USD" : order.getCurrency())
                                + ", tax: " + (order.getTaxAmount() == null ? "0.00" : order.getTaxAmount())
                                + ", paid amount: " + order.getPayAmount() + "."
                        : "订单" + orderNo + "币种：" + (order.getCurrency() == null ? "USD" : order.getCurrency())
                                + "，税费：" + (order.getTaxAmount() == null ? "0.00" : order.getTaxAmount())
                                + "，实付金额：" + order.getPayAmount() + "。";
            }
            return english ? "Order " + orderNo + " status: " + orderStatusEnglish(order.getOrderStatus()) + ", paid amount: " + order.getPayAmount() + "."
                    : "订单" + orderNo + "当前状态：" + orderStatus(order.getOrderStatus()) + "，实付金额：" + order.getPayAmount() + "。";
        } catch (BusinessException ignored) {
            return english ? "That order was not found under your account. Please check the order number."
                    : "未查询到你名下的该订单，请核对订单号。";
        }
    }

    private String extractOrderNo(String message, String plannedOrderNo) {
        Matcher matcher = ORDER_NO.matcher(message);
        if (matcher.find()) return matcher.group();
        return StringUtils.hasText(plannedOrderNo) && ORDER_NO.matcher(plannedOrderNo).matches() ? plannedOrderNo : null;
    }

    private String recentContext(Long memberId, String message, String tool) {
        return recentContext(memberId, message, tool, null);
    }

    private String recentContext(Long memberId, String message, String tool, String plannedKeyword) {
        if ("query_activity".equals(tool)) {
            if (activityService == null) return "";
            List<Activity> activities = activityService.selectActive();
            boolean english = isEnglishMessage(message);
            if (activities.isEmpty()) return english ? "There are no active promotions right now." : "当前没有进行中的活动。";
            String title = english ? "Active promotions：\n" : "当前进行中的活动：\n";
            return title + activities.stream().map(activity -> {
                String name = StringUtils.hasText(activity.getActivityName()) ? activity.getActivityName() : "未命名活动";
                String type = activity.getActivityType();
                return english ? (StringUtils.hasText(type) ? name + " (" + type + ", ends " + activity.getEndTime() + ")"
                                : name + " (ends " + activity.getEndTime() + ")")
                        : (StringUtils.hasText(type) ? name + "（" + type + "，截止 " + activity.getEndTime() + "）"
                                : name + "（截止 " + activity.getEndTime() + "）");
            }).collect(java.util.stream.Collectors.joining("\n"));
        }
        if ("query_product".equals(tool)) {
            String keyword = productKeyword(message, plannedKeyword);
            List<Spu> products = storeCatalogService.products(1, 3, null, keyword.isBlank() ? null : keyword)
                    .getRecords().stream().filter(Spu.class::isInstance).map(Spu.class::cast).toList();
            boolean english = isEnglishMessage(message);
            return products.isEmpty() ? (english ? "No matching products are currently listed." : "暂时没有找到匹配的上架商品。")
                    : (english ? "Matching products:\n" : "为你找到的上架商品：\n") + products.stream().map(product -> productLabel(product, english))
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        if ("query_coupon".equals(tool) && containsAny(message, "我的优惠券", "我的可用优惠券", "券包",
                "my coupons", "my available coupons", "coupon wallet", "coupons do i have")) {
            List<MemberCouponVO> coupons = couponService.listMemberCoupons(memberId).stream()
                    .filter(item -> Integer.valueOf(0).equals(item.getStatus())).toList();
            boolean english = isEnglishMessage(message);
            return coupons.isEmpty() ? (english ? "You currently have no available coupons." : "你目前没有可用优惠券。")
                    : (english ? "Your available coupons:\n" : "你目前有以下可用优惠券：\n") + coupons.stream()
                    .map(item -> english ? item.getCouponName() + " (discount " + item.getDiscount() + ")" : item.getCouponName() + "（减" + item.getDiscount() + "）")
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        if ("query_coupon".equals(tool) && containsAny(message, "可用优惠券", "可领取优惠券", "优惠券有哪些", "有哪些优惠券",
                "available coupons", "what coupons are available", "claimable coupons")) {
            List<Coupon> coupons = couponService.listAvailable();
            boolean english = containsAny(message, "available coupons", "what coupons are available", "claimable coupons");
            if (coupons.isEmpty()) return english ? "There are no claimable coupons right now." : "当前没有可领取的优惠券。";
            String title = english ? "Currently claimable coupons：\n" : "当前可领取的优惠券：\n";
            return title + coupons.stream().map(coupon -> english
                    ? coupon.getCouponName() + " (discount " + coupon.getDiscount() + ", expires " + coupon.getValidEnd() + ")"
                    : coupon.getCouponName() + "（优惠 " + coupon.getDiscount() + "，截止 " + coupon.getValidEnd() + "）")
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        if ("query_coupon".equals(tool)) return "";
        if ("query_member".equals(tool)) {
            Member member = memberService.getById(memberId);
            if (member == null) return "暂未查询到你的会员资料。";
            return isEnglishMessage(message) ? "Your membership level: " + levelNameEnglish(member.getLevel()) + ", points: "
                            + (member.getPoints() == null ? 0 : member.getPoints()) + ", total spend: "
                            + (member.getTotalAmount() == null ? "0.00" : member.getTotalAmount()) + "."
                    : "你的会员等级：" + levelName(member.getLevel()) + "，积分：" + (member.getPoints() == null ? 0 : member.getPoints())
                            + "，累计消费：" + (member.getTotalAmount() == null ? "0.00" : member.getTotalAmount()) + "。";
        }
        if ("query_refund".equals(tool)) {
            if (!containsAny(message, "我的", "查询", "进度", "状态", "申请", "my refund", "refund status", "refund progress",
                    "check my refund", "return status", "request a refund", "i want a refund", "my return")) return "";
            List<TradeRefund> refunds = tradeRefundService.selectMemberPage(memberId, 1, 3).getRecords();
            boolean english = isEnglishMessage(message);
            if (refunds.isEmpty()) return english ? "You currently have no refund requests." : "你目前没有退款申请。";
            return (english ? "Your recent refund requests:\n" : "你最近的退款申请：\n") + refunds.stream()
                    .map(item -> english ? "Order " + item.getOrderNo() + ": " + refundStatusEnglish(item.getRefundStatus())
                            : "订单" + item.getOrderNo() + "：" + refundStatus(item.getRefundStatus()))
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        if ("query_logistics".equals(tool) && !containsAny(message, "我的", "包裹", "订单", "运单号", "my package", "my shipment",
                "tracking number", "track my", "where is my", "delivery status")) return "";
        if ("query_order".equals(tool) && !containsAny(message, "我的订单", "查订单", "订单状态", "订单号", "my order", "check my order",
                "order status", "order number", "order details")) return "";
        if ("query_tax".equals(tool) && !containsAny(message, "我的订单", "订单税费", "税费", "my order", "order tax", "tax on my order",
                "tariff for my order", "customs duty for my order")) return "";
        List<TradeOrder> orders = tradeOrderService.selectPage(1, 3, memberId, null).getRecords();
        boolean english = isEnglishMessage(message);
        if (orders.isEmpty()) return english ? "You currently have no orders." : "你目前还没有订单。";
        if ("query_logistics".equals(tool)) {
            TradeOrder order = orders.get(0);
            TradeLogistics logistics = logisticsService.getByOrderNo(order.getOrderNo());
            return logistics == null ? (english ? "Your latest order is " + order.getOrderNo() + "; no tracking information is available yet."
                            : "你最近的订单是" + order.getOrderNo() + "，当前暂无物流信息。")
                    : (english ? "Your latest order " + order.getOrderNo() + " logistics: " + logistics.getLogisticsCompany() + ", tracking number: " + logistics.getLogisticsNo() + "."
                            : "你最近的订单" + order.getOrderNo() + "的物流：" + logistics.getLogisticsCompany() + "，运单号：" + logistics.getLogisticsNo() + "。");
        }
        return (english ? "Your recent orders:\n" : "你最近的订单：\n") + orders.stream()
                .map(order -> english ? "Order " + order.getOrderNo() + ": " + orderStatusEnglish(order.getOrderStatus()) + ", paid amount: " + order.getPayAmount()
                        : "订单" + order.getOrderNo() + "：" + orderStatus(order.getOrderStatus()) + "，实付金额：" + order.getPayAmount())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String productKeyword(String message, String plannedKeyword) {
        if (StringUtils.hasText(plannedKeyword)) return plannedKeyword;
        return message.replace("查询", "").replace("推荐", "").replace("商品", "")
                .replace("产品", "").replace("找", "").replace("款式", "").replace("有哪些", "").replace("有什么", "")
                .replaceAll("(?i)\\b(?:what|which|are|is|there|any|available|products?|items?|find|search|for|recommend|show|me|please|can|you|have|looking|want|do|a|an|the)\\b", " ")
                .replaceAll("[\\p{Punct}，。！？、]", " ").trim();
    }

    private String orderStatus(Integer status) {
        return switch (status == null ? -1 : status) { case 0 -> "待支付"; case 1 -> "待发货"; case 2 -> "待收货"; case 3 -> "已完成"; case 4 -> "已取消"; case 5 -> "退款处理中"; case 6 -> "已退款"; default -> "处理中"; };
    }

    private String orderStatusEnglish(Integer status) {
        return switch (status == null ? -1 : status) { case 0 -> "pending payment"; case 1 -> "processing"; case 2 -> "awaiting delivery"; case 3 -> "completed"; case 4 -> "cancelled"; case 5 -> "refund processing"; case 6 -> "refunded"; default -> "processing"; };
    }

    private String refundStatus(Integer status) {
        return switch (status == null ? -1 : status) { case 0 -> "待审批"; case 1 -> "已通过"; case 2 -> "已驳回"; case 3 -> "已退款"; case 4 -> "待平台收货"; default -> "处理中"; };
    }

    private String refundStatusEnglish(Integer status) {
        return switch (status == null ? -1 : status) { case 0 -> "pending review"; case 1 -> "approved"; case 2 -> "rejected"; case 3 -> "refunded"; case 4 -> "awaiting return"; default -> "processing"; };
    }

    private String levelName(Integer level) {
        return switch (level == null ? 0 : level) { case 1 -> "Gold 会员"; case 2 -> "Platinum 会员"; default -> "基础会员"; };
    }

    private String levelNameEnglish(Integer level) {
        return switch (level == null ? 0 : level) { case 1 -> "Gold"; case 2 -> "Platinum"; default -> "Basic"; };
    }

    private String productLabel(Spu product, boolean english) {
        String name = StringUtils.hasText(product.getSpuName()) ? product.getSpuName() : "未命名商品";
        if (product.getMinPrice() == null) return name;
        String currency = StringUtils.hasText(product.getCurrency()) ? product.getCurrency() : "USD";
        return english ? name + " (from " + currency + " " + product.getMinPrice() + ")"
                : name + "（起 " + currency + " " + product.getMinPrice() + "）";
    }

    private boolean isEnglishMessage(String message) {
        return message.matches(".*[A-Za-z].*") && !message.matches(".*[\\u4e00-\\u9fff].*");
    }

    private String readText(String event) {
        try {
            JsonNode node = objectMapper.readTree(event);
            return "text".equals(node.path("type").asText()) ? node.path("content").asText() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private String readError(String event) {
        try {
            JsonNode node = objectMapper.readTree(event);
            if (!"error".equals(node.path("type").asText())) return null;
            String message = node.path("message").asText();
            return StringUtils.hasText(message) ? message : "客服服务暂不可用";
        } catch (Exception ignored) {
            return null;
        }
    }

    private void replay(AiConversation message, Consumer<String> eventConsumer) {
        try {
            eventConsumer.accept(objectMapper.writeValueAsString(Map.of("type", "text", "content", message.getContent())));
            eventConsumer.accept(objectMapper.writeValueAsString(Map.of("type", "done")));
        } catch (Exception e) {
            throw new BusinessException("客服回复重放失败");
        }
    }

    void setAuditLogMapper(AiAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    void setActivityService(ActivityService activityService) {
        this.activityService = activityService;
    }

    private void recordAudit(Long memberId, String conversationId, String requestId, String eventType,
                             String toolName, String outcome, int latencyMs, String detail) {
        if (auditLogMapper == null) return;
        try {
            auditLogMapper.insert(memberId, conversationId, requestId, eventType, toolName, outcome, latencyMs, detail);
        } catch (RuntimeException e) {
            log.warn("AI audit log write failed", e);
        }
    }

    private void save(Long memberId, String conversationId, String requestId, String role, String content, int tokens, int latencyMs) {
        AiConversation message = new AiConversation();
        message.setUserId(memberId); message.setSessionId(conversationId); message.setRequestId(requestId); message.setRole(role); message.setContent(content);
        message.setTokensUsed(tokens); message.setLatencyMs(latencyMs); message.setModel("customer-service-p0");
        conversationMapper.insert(message);
    }
}
