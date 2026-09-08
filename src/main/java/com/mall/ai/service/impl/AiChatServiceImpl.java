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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Override
    public void stream(Long memberId, String conversationId, AiChatRequest request, Consumer<String> eventConsumer) {
        save(memberId, conversationId, "user", request.getMessage(), 0, 0);
        List<AiConversation> history = conversationMapper.selectRecent(memberId, conversationId, 10);
        StringBuilder answer = new StringBuilder();
        long start = System.currentTimeMillis();
        gatewayClient.stream(memberId, conversationId, request.getMessage(), businessContext(memberId, request.getMessage()), history, event -> {
            answer.append(readText(event));
            eventConsumer.accept(event);
        });
        save(memberId, conversationId, "assistant", answer.toString(), 0, (int) (System.currentTimeMillis() - start));
    }

    private String businessContext(Long memberId, String message) {
        Matcher matcher = ORDER_NO.matcher(message);
        if (!matcher.find()) return "";
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
            return "订单" + orderNo + "当前状态：" + orderStatus(order.getOrderStatus()) + "，实付金额：" + order.getPayAmount() + "。";
        } catch (BusinessException ignored) {
            return "未查询到你名下的该订单，请核对订单号。";
        }
    }

    private String orderStatus(Integer status) {
        return switch (status == null ? -1 : status) { case 0 -> "待支付"; case 1 -> "待发货"; case 2 -> "待收货"; case 3 -> "已完成"; case 4 -> "已取消"; case 5 -> "退款处理中"; case 6 -> "已退款"; default -> "处理中"; };
    }

    private String refundStatus(Integer status) {
        return switch (status == null ? -1 : status) { case 0 -> "待审批"; case 1 -> "已通过"; case 2 -> "已驳回"; case 3 -> "已退款"; default -> "处理中"; };
    }

    private String readText(String event) {
        try {
            JsonNode node = objectMapper.readTree(event);
            return "text".equals(node.path("type").asText()) ? node.path("content").asText() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private void save(Long memberId, String conversationId, String role, String content, int tokens, int latencyMs) {
        AiConversation message = new AiConversation();
        message.setUserId(memberId); message.setSessionId(conversationId); message.setRole(role); message.setContent(content);
        message.setTokensUsed(tokens); message.setLatencyMs(latencyMs); message.setModel("customer-service-p0");
        conversationMapper.insert(message);
    }
}
