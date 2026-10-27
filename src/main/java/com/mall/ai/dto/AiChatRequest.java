package com.mall.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI聊天请求实体类
 * 用于封装AI聊天接口的请求参数
 * 包含会话ID、请求ID和消息内容等字段
 */
@Data
public class AiChatRequest {
    /**
     * 会话ID
     * 用于标识一次完整的对话会话
     * 最大长度为64个字符
     */
    @Size(max = 64)
    private String conversationId;
    /**
     * 请求ID
     * 用于标识本次请求的唯一标识
     * 不能为空，最大长度为64个字符
     */
    @NotBlank
    @Size(max = 64)
    private String requestId;
    /**
     * 聊天消息内容
     * 用户发送的消息内容
     * 不能为空，最大长度为1000个字符
     */
    @NotBlank
    @Size(max = 1000)
    private String message;
}
