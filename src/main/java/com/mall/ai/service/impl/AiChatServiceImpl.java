package com.mall.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.ai.dto.AiChatRequest;
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
import com.mall.marketing.service.CouponService;
import com.mall.member.entity.Member;
import com.mall.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
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
                else {
                    recordAudit(memberId, conversationId, requestId, "chat", null, "in_progress", 0, "duplicate_request");
                    eventConsumer.accept("{\"type\":\"error\",\"message\":\"客服请求正在处理中，请稍后重试\"}");
                }
                return;
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
            if (businessContext.isBlank()) {
                Map<String, Object> decision = gatewayClient.plan(memberId, conversationId, request.getMessage(), history);
                String plannedTool = plannedTool(decision);
                String plannedContext = plannedTool.isBlank() ? "" : businessContext(memberId, request.getMessage(), plannedTool, plannedOrderNo(decision));
                if (!plannedContext.isBlank()) {
                    businessContext = plannedContext;
                    selectedTool = plannedTool;
                    recordAudit(memberId, conversationId, requestId, "tool_plan", selectedTool, "accepted", 0, "model_read_only_plan");
                }
            }
            if (!selectedTool.isBlank() && !businessContext.isBlank()) {
                recordAudit(memberId, conversationId, requestId, "tool", selectedTool, "executed", 0, "read_only");
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

    private String businessTool(String message, String context) {
        if (context.isBlank()) return "";
        return intent(message);
    }

    private String intent(String message) {
        if (containsAny(message, "物流", "快递", "运单", "包裹", "配送", "追踪", "轨迹", "tracking", "track")) return "query_logistics";
        if (containsAny(message, "退款", "退货", "售后", "退钱")) return "query_refund";
        if (containsAny(message, "会员", "积分", "等级", "成长", "成长值")) return "query_member";
        if (containsAny(message, "税费", "关税", "税金", "币种")) return "query_tax";
        if (containsAny(message, "优惠券", "券包", "折扣券")) return "query_coupon";
        if (containsAny(message, "商品", "产品", "推荐", "找", "款式", "规格")) return "query_product";
        return "query_order";
    }

    private boolean containsAny(String message, String... keywords) {
        return java.util.Arrays.stream(keywords).anyMatch(message::contains);
    }

    private String plannedTool(Map<String, Object> decision) {
        Object value = decision == null ? null : decision.get("tool");
        return value instanceof String tool && isSupportedTool(tool) ? tool : "";
    }

    private String plannedOrderNo(Map<String, Object> decision) {
        Object arguments = decision == null ? null : decision.get("arguments");
        if (arguments instanceof Map<?, ?> values && values.get("order_no") instanceof String orderNo) return orderNo;
        return null;
    }

    private boolean isSupportedTool(String tool) {
        return switch (tool) {
            case "query_order", "query_logistics", "query_refund", "query_product", "query_coupon", "query_member", "query_tax" -> true;
            default -> false;
        };
    }

    private String businessContext(Long memberId, String message) {
        return businessContext(memberId, message, intent(message), null);
    }

    private String businessContext(Long memberId, String message, String tool, String plannedOrderNo) {
        String orderNo = extractOrderNo(message, plannedOrderNo);
        if (orderNo == null) return recentContext(memberId, message, tool);
        try {
            TradeOrder order = tradeOrderService.getOwnedByOrderNo(orderNo, memberId);
            if ("query_logistics".equals(tool)) {
                TradeLogistics logistics = logisticsService.getByOrderNo(orderNo);
                return logistics == null ? "订单" + orderNo + "暂未录入物流信息。"
                        : "订单" + orderNo + "的物流：" + logistics.getLogisticsCompany() + "，运单号：" + logistics.getLogisticsNo() + "。";
            }
            if ("query_refund".equals(tool)) {
                TradeRefund refund = tradeRefundService.selectMemberPage(memberId, 1, 100).getRecords().stream()
                        .filter(item -> orderNo.equals(item.getOrderNo())).findFirst().orElse(null);
                return refund == null ? "订单" + orderNo + "当前没有退款申请，订单状态：" + orderStatus(order.getOrderStatus()) + "。"
                        : "订单" + orderNo + "的退款状态：" + refundStatus(refund.getRefundStatus()) + "。";
            }
            if ("query_tax".equals(tool)) {
                return "订单" + orderNo + "币种：" + (order.getCurrency() == null ? "USD" : order.getCurrency())
                        + "，税费：" + (order.getTaxAmount() == null ? "0.00" : order.getTaxAmount())
                        + "，实付金额：" + order.getPayAmount() + "。";
            }
            return "订单" + orderNo + "当前状态：" + orderStatus(order.getOrderStatus()) + "，实付金额：" + order.getPayAmount() + "。";
        } catch (BusinessException ignored) {
            return "未查询到你名下的该订单，请核对订单号。";
        }
    }

    private String extractOrderNo(String message, String plannedOrderNo) {
        Matcher matcher = ORDER_NO.matcher(message);
        if (matcher.find()) return matcher.group();
        return StringUtils.hasText(plannedOrderNo) && ORDER_NO.matcher(plannedOrderNo).matches() ? plannedOrderNo : null;
    }

    private String recentContext(Long memberId, String message, String tool) {
        if ("query_product".equals(tool)) {
            String keyword = message.replace("查询", "").replace("推荐", "").replace("商品", "")
                    .replace("产品", "").replace("找", "").replace("款式", "").replace("有哪些", "").replace("有什么", "").trim();
            List<Spu> products = storeCatalogService.products(1, 3, null, keyword.isBlank() ? null : keyword)
                    .getRecords().stream().filter(Spu.class::isInstance).map(Spu.class::cast).toList();
            return products.isEmpty() ? "暂时没有找到匹配的上架商品。"
                    : "为你找到的上架商品：\n" + products.stream().map(Spu::getSpuName)
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        if ("query_coupon".equals(tool) && containsAny(message, "我的优惠券", "可用优惠券", "优惠券有哪些", "券包")) {
            List<MemberCouponVO> coupons = couponService.listMemberCoupons(memberId).stream()
                    .filter(item -> Integer.valueOf(0).equals(item.getStatus())).toList();
            return coupons.isEmpty() ? "你目前没有可用优惠券。"
                    : "你目前有以下可用优惠券：\n" + coupons.stream()
                    .map(item -> item.getCouponName() + "（减" + item.getDiscount() + "）")
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        if ("query_member".equals(tool)) {
            Member member = memberService.getById(memberId);
            if (member == null) return "暂未查询到你的会员资料。";
            return "你的会员等级：" + levelName(member.getLevel()) + "，积分：" + (member.getPoints() == null ? 0 : member.getPoints())
                    + "，累计消费：" + (member.getTotalAmount() == null ? "0.00" : member.getTotalAmount()) + "。";
        }
        if ("query_refund".equals(tool)) {
            if (!(message.contains("我的") || message.contains("查询") || message.contains("进度") || message.contains("状态") || message.contains("申请"))) return "";
            List<TradeRefund> refunds = tradeRefundService.selectMemberPage(memberId, 1, 3).getRecords();
            if (refunds.isEmpty()) return "你目前没有退款申请。";
            return "你最近的退款申请：\n" + refunds.stream()
                    .map(item -> "订单" + item.getOrderNo() + "：" + refundStatus(item.getRefundStatus()))
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        if ("query_logistics".equals(tool) && !containsAny(message, "我的", "包裹", "订单", "运单号")) return "";
        if ("query_order".equals(tool) && !containsAny(message, "我的订单", "查订单", "订单状态", "订单号")) return "";
        List<TradeOrder> orders = tradeOrderService.selectPage(1, 3, memberId, null).getRecords();
        if (orders.isEmpty()) return "你目前还没有订单。";
        if ("query_logistics".equals(tool)) {
            TradeOrder order = orders.get(0);
            TradeLogistics logistics = logisticsService.getByOrderNo(order.getOrderNo());
            return logistics == null ? "你最近的订单是" + order.getOrderNo() + "，当前暂无物流信息。"
                    : "你最近的订单" + order.getOrderNo() + "的物流：" + logistics.getLogisticsCompany() + "，运单号：" + logistics.getLogisticsNo() + "。";
        }
        return "你最近的订单：\n" + orders.stream()
                .map(order -> "订单" + order.getOrderNo() + "：" + orderStatus(order.getOrderStatus()) + "，实付金额：" + order.getPayAmount())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String orderStatus(Integer status) {
        return switch (status == null ? -1 : status) { case 0 -> "待支付"; case 1 -> "待发货"; case 2 -> "待收货"; case 3 -> "已完成"; case 4 -> "已取消"; case 5 -> "退款处理中"; case 6 -> "已退款"; default -> "处理中"; };
    }

    private String refundStatus(Integer status) {
        return switch (status == null ? -1 : status) { case 0 -> "待审批"; case 1 -> "已通过"; case 2 -> "已驳回"; case 3 -> "已退款"; case 4 -> "待平台收货"; default -> "处理中"; };
    }

    private String levelName(Integer level) {
        return switch (level == null ? 0 : level) { case 1 -> "Gold 会员"; case 2 -> "Platinum 会员"; default -> "基础会员"; };
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
