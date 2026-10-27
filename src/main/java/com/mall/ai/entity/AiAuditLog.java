package com.mall.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI审计日志实体类
 * 用于记录AI相关操作的审计信息
 */
@Data
public class AiAuditLog {
    private Long id;                    // 主键ID
    private Long userId;               // 用户ID
    private String sessionId;         // 会话ID
    private String requestId;         // 请求ID
    private String eventType;         // 事件类型
    private String toolName;          // 工具名称
    private String outcome;           // 操作结果
    private Integer latencyMs;        // 延迟时间(毫秒)
    private String detail;            // 详细信息
    private LocalDateTime createTime; // 创建时间
}
