package com.mall.ai.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface AiAuditLogMapper {
    @Insert("INSERT INTO ai_audit_log "
            + "(user_id, session_id, request_id, event_type, tool_name, outcome, latency_ms, detail, create_time) "
            + "VALUES (#{userId}, #{sessionId}, #{requestId}, #{eventType}, #{toolName}, #{outcome}, #{latencyMs}, #{detail}, NOW())")
    int insert(@Param("userId") Long userId, @Param("sessionId") String sessionId,
               @Param("requestId") String requestId, @Param("eventType") String eventType,
               @Param("toolName") String toolName, @Param("outcome") String outcome,
               @Param("latencyMs") int latencyMs, @Param("detail") String detail);
}
