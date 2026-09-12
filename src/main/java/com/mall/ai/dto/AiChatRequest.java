package com.mall.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRequest {
    @Size(max = 64)
    private String conversationId;
    @Size(max = 64)
    private String requestId;
    @NotBlank
    @Size(max = 1000)
    private String message;
}
