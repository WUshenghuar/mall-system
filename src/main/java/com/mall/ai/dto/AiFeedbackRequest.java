package com.mall.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI反馈请求类，用于接收用户对AI回复的反馈信息
 * 包含消息ID和反馈值两个字段
 */
@Data
public class AiFeedbackRequest {
    /**
     * 消息ID，必填字段
     * 用于标识需要反馈的具体消息
     */
    @NotNull
    private Long messageId;

    @NotNull
    @Min(-1)
    @Max(1)
    private Integer feedback;
}
