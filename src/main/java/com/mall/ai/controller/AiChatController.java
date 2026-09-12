package com.mall.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.result.Result;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.dto.AiFeedbackRequest;
import com.mall.ai.entity.AiAuditLog;
import com.mall.ai.entity.AiConversation;
import com.mall.ai.service.AiChatService;
import com.mall.security.user.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {
    private static final DefaultRedisScript<Long> RATE_LIMIT = new DefaultRedisScript<>(
            "local current=redis.call('GET',KEYS[1]); if not current or current==ARGV[1] then "
                    + "redis.call('SET',KEYS[1],ARGV[1],'EX',ARGV[2]); return 1 end; return 0", Long.class);
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;
    private final AsyncTaskExecutor aiChatExecutor;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request, Authentication auth) {
        Long memberId = CurrentMember.id(auth);
        if (!allow(memberId, request.getRequestId())) {
            SseEmitter limited = new SseEmitter(60_000L);
            send(limited, Map.of("type", "error", "message", "客服请求过于频繁，请稍后重试"));
            limited.complete();
            return limited;
        }
        String conversationId = StringUtils.hasText(request.getConversationId()) ? request.getConversationId() : UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(60_000L);
        AtomicBoolean active = new AtomicBoolean(true);
        AtomicReference<Future<?>> taskRef = new AtomicReference<>();
        Runnable cancel = () -> {
            if (!active.getAndSet(false)) return;
            Future<?> task = taskRef.get();
            if (task != null && !task.isDone()) task.cancel(true);
        };
        emitter.onCompletion(cancel);
        emitter.onTimeout(() -> { cancel.run(); emitter.complete(); });
        emitter.onError(error -> { cancel.run(); emitter.complete(); });
        try {
            taskRef.set(aiChatExecutor.submit(() -> {
                if (!active.get()) return;
                try {
                    send(emitter, Map.of("type", "meta", "conversationId", conversationId));
                    if (!active.get()) return;
                    aiChatService.stream(memberId, conversationId, request, event -> {
                        if (!active.get()) throw new CancellationException();
                        sendRaw(emitter, event);
                    });
                    if (!active.get()) throw new CancellationException();
                } catch (CancellationException ignored) {
                } catch (Exception e) {
                    if (active.get()) send(emitter, Map.of("type", "error", "message", e.getMessage() == null ? "客服服务暂不可用" : e.getMessage()));
                } finally {
                    if (active.compareAndSet(true, false)) emitter.complete();
                }
            }));
        } catch (RuntimeException e) {
            cancel.run();
            send(emitter, Map.of("type", "error", "message", "客服当前繁忙，请稍后重试"));
            emitter.complete();
        }
        return emitter;
    }

    private boolean allow(Long memberId, String requestId) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) return true;
        try {
            Long result = redis.execute(RATE_LIMIT, List.of("ai:chat:rate:" + memberId),
                    StringUtils.hasText(requestId) ? requestId : "no-request-id", "2");
            return Long.valueOf(1L).equals(result);
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    @GetMapping("/recent")
    public Result<List<AiConversation>> recent(Authentication auth) {
        return Result.success(aiChatService.recent(CurrentMember.id(auth), 20));
    }

    @PostMapping("/feedback")
    public Result<Void> feedback(@Valid @RequestBody AiFeedbackRequest request, Authentication auth) {
        aiChatService.feedback(CurrentMember.id(auth), request.getMessageId(), request.getFeedback());
        return Result.success(null);
    }

    @GetMapping("/feedback/stats")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('order:support:list')")
    public Result<Map<String, Object>> feedbackStats() {
        return Result.success(aiChatService.feedbackStats());
    }

    @GetMapping("/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('order:support:list')")
    public Result<Map<String, Object>> runtimeStatus() {
        return Result.success(aiChatService.runtimeStatus());
    }

    @GetMapping("/audit")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('order:support:list')")
    public Result<IPage<AiAuditLog>> audit(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") Integer page,
                                           @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") Integer size,
                                           @org.springframework.web.bind.annotation.RequestParam(required = false) String eventType,
                                           @org.springframework.web.bind.annotation.RequestParam(required = false) String outcome) {
        return Result.success(aiChatService.auditPage(page, size, eventType, outcome));
    }

    private void send(SseEmitter emitter, Map<String, Object> event) {
        try { sendRaw(emitter, objectMapper.writeValueAsString(event)); } catch (Exception ignored) { }
    }

    private void sendRaw(SseEmitter emitter, String event) {
        try { emitter.send(SseEmitter.event().name("message").data(event)); } catch (Exception ignored) { }
    }
}
