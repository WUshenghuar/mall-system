package com.mall.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.service.AiChatService;
import com.mall.security.user.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request, Authentication auth) {
        Long memberId = CurrentMember.id(auth);
        String conversationId = StringUtils.hasText(request.getConversationId()) ? request.getConversationId() : UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(60_000L);
        CompletableFuture.runAsync(() -> {
            try {
                send(emitter, Map.of("type", "meta", "conversationId", conversationId));
                aiChatService.stream(memberId, conversationId, request, event -> sendRaw(emitter, event));
            } catch (Exception e) {
                send(emitter, Map.of("type", "error", "message", e.getMessage() == null ? "客服服务暂不可用" : e.getMessage()));
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    private void send(SseEmitter emitter, Map<String, Object> event) {
        try { sendRaw(emitter, objectMapper.writeValueAsString(event)); } catch (Exception ignored) { }
    }

    private void sendRaw(SseEmitter emitter, String event) {
        try { emitter.send(SseEmitter.event().name("message").data(event)); } catch (Exception ignored) { }
    }
}
