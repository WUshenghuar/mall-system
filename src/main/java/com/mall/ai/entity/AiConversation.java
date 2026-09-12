package com.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation")
public class AiConversation extends BaseEntity {
    private String sessionId;
    private String requestId;
    private Long userId;
    /** user / assistant / agent */
    private String role;
    private String content;
    private Integer tokensUsed;
    private Integer latencyMs;
    private String model;
    private Integer feedback;
}
