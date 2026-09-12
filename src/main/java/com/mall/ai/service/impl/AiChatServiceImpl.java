package com.mall.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.entity.AiConversation;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
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

    @Override
    public void stream(Long memberId, String conversationId, AiChatRequest request, Consumer<String> eventConsumer) {
        String requestId = StringUtils.hasText(request.getRequestId()) ? request.getRequestId().trim() : null;
        if (requestId != null) {
            AiConversation previous = conversationMapper.selectAssistantByRequest(memberId, conversationId, requestId);
            if (previous != null) {
                replay(previous, eventConsumer);
                return;
            }
            if (conversationMapper.insertUserIfAbsent(conversationId, requestId, memberId, request.getMessage()) != 1) {
                previous = conversationMapper.selectAssistantByRequest(memberId, conversationId, requestId);
                if (previous != null) replay(previous, eventConsumer);
                else eventConsumer.accept("{\"type\":\"error\",\"message\":\"客服请求正在处理中，请稍后重试\"}");
                return;
            }
        } else {
            save(memberId, conversationId, null, "user", request.getMessage(), 0, 0);
        }
        List<AiConversation> history = conversationMapper.selectRecent(memberId, conversationId, 10);
        StringBuilder answer = new StringBuilder();
        long start = System.currentTimeMillis();
        String businessContext = businessContext(memberId, request.getMessage());
        try {
            gatewayClient.stream(memberId, conversationId, request.getMessage(), businessContext,
                    businessTool(request.getMessage(), businessContext), history, event -> {
                answer.append(readText(event));
                eventConsumer.accept(event);
            });
            save(memberId, conversationId, requestId, "assistant", answer.toString(), 0, (int) (System.currentTimeMillis() - start));
        } catch (RuntimeException e) {
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
        if (message.contains("物流") || message.contains("快递") || message.contains("运单")) return "query_logistics";
        if (message.contains("退款") || message.contains("售后")) return "query_refund";
        if (message.contains("会员") || message.contains("积分") || message.contains("等级") || message.contains("成长")) return "query_member";
        if (message.contains("税费") || message.contains("关税") || message.contains("币种")) return "query_tax";
        if (message.contains("商品") || message.contains("产品") || message.contains("推荐") || message.contains("找")) return "query_product";
        if (message.contains("优惠券") || message.contains("券包")) return "query_coupon";
        return "query_order";
    }

    private String businessContext(Long memberId, String message) {
        Matcher matcher = ORDER_NO.matcher(message);
        if (!matcher.find()) return recentContext(memberId, message);
        String orderNo = matcher.group();
        try {
            TradeOrder order = tradeOrderService.getOwnedByOrderNo(orderNo, memberId);
            if (message.contains("物流") || message.contains("快递") || message.contains("运单")) {
                TradeLogistics logistics = logisticsService.getByOrderNo(orderNo);
                return logistics == null ? "订单" + orderNo + "暂未录入物流信息。"
                        : "订单" + orderNo + "的物流：" + logistics.getLogisticsCompany() + "，运单号：" + logistics.getLogisticsNo() + "。";
            }
            if (message.contains("退款") || message.contains("售后")) {
                TradeRefund refund = tradeRefundService.selectMemberPage(memberId, 1, 100).getRecords().stream()
                        .filter(item -> orderNo.equals(item.getOrderNo())).findFirst().orElse(null);
                return refund == null ? "订单" + orderNo + "当前没有退款申请，订单状态：" + orderStatus(order.getOrderStatus()) + "。"
                        : "订单" + orderNo + "的退款状态：" + refundStatus(refund.getRefundStatus()) + "。";
            }
            if (message.contains("税费") || message.contains("关税") || message.contains("币种")) {
                return "订单" + orderNo + "币种：" + (order.getCurrency() == null ? "USD" : order.getCurrency())
                        + "，税费：" + (order.getTaxAmount() == null ? "0.00" : order.getTaxAmount())
                        + "，实付金额：" + order.getPayAmount() + "。";
            }
            return "订单" + orderNo + "当前状态：" + orderStatus(order.getOrderStatus()) + "，实付金额：" + order.getPayAmount() + "。";
        } catch (BusinessException ignored) {
            return "未查询到你名下的该订单，请核对订单号。";
        }
    }

    private String recentContext(Long memberId, String message) {
        if (message.contains("商品") || message.contains("产品") || message.contains("推荐") || message.contains("找")) {
            String keyword = message.replace("查询", "").replace("推荐", "").replace("商品", "")
                    .replace("产品", "").replace("有哪些", "").replace("有什么", "").trim();
            List<Spu> products = storeCatalogService.products(1, 3, null, keyword.isBlank() ? null : keyword)
                    .getRecords().stream().filter(Spu.class::isInstance).map(Spu.class::cast).toList();
            return products.isEmpty() ? "暂时没有找到匹配的上架商品。"
                    : "为你找到的上架商品：\n" + products.stream().map(Spu::getSpuName)
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        if (message.contains("我的优惠券") || message.contains("可用优惠券") || message.contains("优惠券有哪些") || message.contains("券包")) {
            List<MemberCouponVO> coupons = couponService.listMemberCoupons(memberId).stream()
                    .filter(item -> Integer.valueOf(0).equals(item.getStatus())).toList();
            return coupons.isEmpty() ? "你目前没有可用优惠券。"
                    : "你目前有以下可用优惠券：\n" + coupons.stream()
                    .map(item -> item.getCouponName() + "（减" + item.getDiscount() + "）")
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        if (message.contains("会员") || message.contains("积分") || message.contains("等级") || message.contains("成长")) {
            Member member = memberService.getById(memberId);
            if (member == null) return "暂未查询到你的会员资料。";
            return "你的会员等级：" + levelName(member.getLevel()) + "，积分：" + (member.getPoints() == null ? 0 : member.getPoints())
                    + "，累计消费：" + (member.getTotalAmount() == null ? "0.00" : member.getTotalAmount()) + "。";
        }
        if (message.contains("退款") || message.contains("售后")) {
            if (!(message.contains("我的") || message.contains("查询") || message.contains("进度") || message.contains("状态") || message.contains("申请"))) return "";
            List<TradeRefund> refunds = tradeRefundService.selectMemberPage(memberId, 1, 3).getRecords();
            if (refunds.isEmpty()) return "你目前没有退款申请。";
            return "你最近的退款申请：\n" + refunds.stream()
                    .map(item -> "订单" + item.getOrderNo() + "：" + refundStatus(item.getRefundStatus()))
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        if (!(message.contains("我的订单") || message.contains("查订单") || message.contains("订单状态") || message.contains("订单号"))) return "";
        List<TradeOrder> orders = tradeOrderService.selectPage(1, 3, memberId, null).getRecords();
        if (orders.isEmpty()) return "你目前还没有订单。";
        if (message.contains("物流") || message.contains("快递") || message.contains("运单")) {
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

    private void replay(AiConversation message, Consumer<String> eventConsumer) {
        try {
            eventConsumer.accept(objectMapper.writeValueAsString(Map.of("type", "text", "content", message.getContent())));
            eventConsumer.accept(objectMapper.writeValueAsString(Map.of("type", "done")));
        } catch (Exception e) {
            throw new BusinessException("客服回复重放失败");
        }
    }

    private void save(Long memberId, String conversationId, String requestId, String role, String content, int tokens, int latencyMs) {
        AiConversation message = new AiConversation();
        message.setUserId(memberId); message.setSessionId(conversationId); message.setRequestId(requestId); message.setRole(role); message.setContent(content);
        message.setTokensUsed(tokens); message.setLatencyMs(latencyMs); message.setModel("customer-service-p0");
        conversationMapper.insert(message);
    }
}
