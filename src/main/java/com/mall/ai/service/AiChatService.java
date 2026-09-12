package com.mall.ai.service;

import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.entity.AiConversation;

import java.util.List;
import java.util.function.Consumer;

public interface AiChatService {
    void stream(Long memberId, String conversationId, AiChatRequest request, Consumer<String> eventConsumer);
    List<AiConversation> recent(Long memberId, int limit);
}
