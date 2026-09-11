package com.mall.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.ai.entity.AiConversation;
import com.mall.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiGatewayClient {
    private final ObjectMapper objectMapper;
    @Value("${ai.service.base-url:http://localhost:8101}") private String baseUrl;
    @Value("${ai.service.token:change-me-local-only}") private String serviceToken;

    public void stream(Long memberId, String conversationId, String message, String businessContext, String businessTool, List<AiConversation> history,
                       Consumer<String> eventConsumer) {
        try {
            List<Map<String, String>> messages = history.stream()
                    .map(item -> Map.of("role", item.getRole(), "content", item.getContent())).toList();
            String body = objectMapper.writeValueAsString(Map.of("memberId", memberId, "conversationId", conversationId,
                    "message", message, "businessContext", businessContext, "businessTool", businessTool, "history", messages));
            HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl + "/internal/chat").toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(45).toMillis());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-AI-Service-Token", serviceToken);
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
}
