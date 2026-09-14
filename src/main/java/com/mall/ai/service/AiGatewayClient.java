package com.mall.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.ai.dto.AiKnowledgeDocument;
import com.mall.ai.entity.AiConversation;
import com.mall.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiGatewayClient {
    private final ObjectMapper objectMapper;
    @Value("${ai.service.base-url:http://localhost:8101}") private String baseUrl;
    @Value("${ai.service.token:change-me-local-only}") private String serviceToken;

    public Map<String, Object> runtimeStatus() {
        try {
            return objectMapper.convertValue(requestJson("GET", "/health", null), new TypeReference<>() { });
        } catch (Exception e) {
            log.debug("AI runtime status unavailable", e);
            return Map.of("status", "unavailable", "enabled", false, "modelConfigured", false, "modelUsageEnabled", false, "embeddingConfigured", false,
                    "rerankerConfigured", false, "langfuseConfigured", false, "toolPlanningEnabled", false);
        }
    }

    public Map<String, Object> runtimeMetrics() {
        try {
            return objectMapper.convertValue(requestJson("GET", "/internal/metrics", null), new TypeReference<>() { });
        } catch (Exception e) {
            log.debug("AI runtime metrics unavailable", e);
            return Map.of("status", "unavailable");
        }
    }

    public Map<String, Object> plan(Long memberId, String conversationId, String message, List<AiConversation> history) {
        return plan(memberId, conversationId, message, history, List.of());
    }

    public Map<String, Object> plan(Long memberId, String conversationId, String message, List<AiConversation> history,
                                    List<String> toolResults) {
        try {
            List<Map<String, String>> messages = history.stream()
                    .map(item -> Map.of("role", "agent".equals(item.getRole()) ? "assistant" : item.getRole(), "content", item.getContent())).toList();
            JsonNode response = requestJson("POST", "/internal/tool-plan", Map.of(
                    "memberId", memberId, "conversationId", conversationId, "message", message, "history", messages,
                    "toolResults", toolResults == null ? List.of() : toolResults), 2,
                    traceId(conversationId, message), traceParent(conversationId, message));
            return objectMapper.convertValue(response, new TypeReference<>() { });
        } catch (Exception e) {
            log.debug("AI tool planning unavailable; using deterministic routing", e);
            return Map.of();
        }
    }

    public void stream(Long memberId, String conversationId, String message, String businessContext, String businessTool, List<AiConversation> history,
                       Consumer<String> eventConsumer) {
        try {
            List<Map<String, String>> messages = history.stream()
                    .map(item -> Map.of("role", "agent".equals(item.getRole()) ? "assistant" : item.getRole(), "content", item.getContent())).toList();
            String body = objectMapper.writeValueAsString(Map.of("memberId", memberId, "conversationId", conversationId,
                    "message", message, "businessContext", businessContext, "businessTool", businessTool, "history", messages));
            HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl + "/internal/chat").toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(45).toMillis());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-AI-Service-Token", serviceToken);
            connection.setRequestProperty("X-Trace-Id", traceId(conversationId, message));
            connection.setRequestProperty("traceparent", traceParent(conversationId, message));
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) { output.write(body.getBytes(StandardCharsets.UTF_8)); }
            if (connection.getResponseCode() != 200) throw new BusinessException("客服服务暂不可用");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) eventConsumer.accept(line.substring(5).trim());
                }
            } finally {
                connection.disconnect();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI service request failed", e);
            throw new BusinessException("客服服务连接失败，请稍后重试");
        }
    }

    public List<AiKnowledgeDocument> listKnowledge() {
        try {
            return objectMapper.convertValue(requestJson("GET", "/internal/knowledge", null).path("items"),
                    new TypeReference<>() { });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI knowledge list failed", e);
            throw new BusinessException("客服知识库连接失败");
        }
    }

    public AiKnowledgeDocument saveKnowledge(AiKnowledgeDocument document) {
        try {
            String id = URLEncoder.encode(document.getId(), StandardCharsets.UTF_8);
            return objectMapper.convertValue(requestJson("PUT", "/internal/knowledge/" + id, document),
                    AiKnowledgeDocument.class);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI knowledge save failed", e);
            throw new BusinessException("客服知识库连接失败");
        }
    }

    private JsonNode requestJson(String method, String path, Object body) throws Exception {
        return requestJson(method, path, body, 10);
    }

    private JsonNode requestJson(String method, String path, Object body, int timeoutSeconds) throws Exception {
        return requestJson(method, path, body, timeoutSeconds, null, null);
    }

    private JsonNode requestJson(String method, String path, Object body, int timeoutSeconds,
                                 String traceId, String traceParent) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl + path).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());
        connection.setReadTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-AI-Service-Token", serviceToken);
        if (traceId != null) connection.setRequestProperty("X-Trace-Id", traceId);
        if (traceParent != null) connection.setRequestProperty("traceparent", traceParent);
        if (body != null) {
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(objectMapper.writeValueAsBytes(body));
            }
        }
        try {
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String response = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (status != 200) throw new BusinessException("客服知识库服务暂不可用");
            return response.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response);
        } finally {
            connection.disconnect();
        }
    }

    private String traceId(String conversationId, String message) {
        return UUID.nameUUIDFromBytes((conversationId + "\n" + message).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
    }

    private String traceParent(String conversationId, String message) {
        return "00-" + traceId(conversationId, message) + "-0000000000000001-01";
    }
}
