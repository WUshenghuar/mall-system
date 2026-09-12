package com.mall.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.ai.dto.AiChatRequest;
import com.mall.ai.entity.AiAuditLog;
import com.mall.ai.entity.AiConversation;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface AiChatService {
    void stream(Long memberId, String conversationId, AiChatRequest request, Consumer<String> eventConsumer);
    List<AiConversation> recent(Long memberId, int limit);
    void feedback(Long memberId, Long messageId, Integer feedback);
    Map<String, Object> feedbackStats();
    IPage<AiAuditLog> auditPage(Integer page, Integer size, String eventType, String outcome);
}
