package com.mall.ai.service;

import com.mall.ai.dto.AiChatRequest;

import java.util.function.Consumer;

public interface AiChatService {
    void stream(Long memberId, String conversationId, AiChatRequest request, Consumer<String> eventConsumer);
}
