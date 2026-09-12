package com.mall.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiAuditLog {
    private Long id;
    private Long userId;
    private String sessionId;
    private String requestId;
    private String eventType;
    private String toolName;
    private String outcome;
    private Integer latencyMs;
    private String detail;
    private LocalDateTime createTime;
}
