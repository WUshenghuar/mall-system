package com.mall.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.service.AiChatService;
import com.mall.security.user.MemberPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;

class AiChatControllerTest {
    @Test
    void rejectsRapidDifferentRequestsBeforeSchedulingAiWork() {
        AiChatService service = mock(AiChatService.class);
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any())).thenReturn(0L);
        AiChatController controller = new AiChatController(service, new ObjectMapper(), syncExecutor(), provider);

        controller.chat(request("req-2"), memberAuth());

        verifyNoInteractions(service);
    }

    @Test
    void schedulesAndStreamsAcceptedRequest() {
        AiChatService service = mock(AiChatService.class);
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        doAnswer(invocation -> {
            Consumer<String> events = invocation.getArgument(3);
            events.accept("{\"type\":\"text\",\"content\":\"ok\"}");
            return null;
        }).when(service).stream(any(), any(), any(), any());
        AiChatController controller = new AiChatController(service, new ObjectMapper(), syncExecutor(), provider);

        controller.chat(request("req-1"), memberAuth());

        verify(service).stream(any(), any(), any(), any());
    }

    private AiChatRequest request(String requestId) {
        AiChatRequest request = new AiChatRequest();
        request.setRequestId(requestId);
        request.setMessage("你好");
        return request;
    }

    private AsyncTaskExecutor syncExecutor() {
        AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
        when(executor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return CompletableFuture.completedFuture(null);
        });
        return executor;
    }

    private Authentication memberAuth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new MemberPrincipal(9L, "13900000001", "", 1));
        return auth;
    }
}
