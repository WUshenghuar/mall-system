package com.mall.ai.mapper;

import com.mall.ai.entity.AiAuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AiAuditLogMapper {
    @Insert("INSERT INTO ai_audit_log "
            + "(user_id, session_id, request_id, event_type, tool_name, outcome, latency_ms, detail, create_time) "
            + "VALUES (#{userId}, #{sessionId}, #{requestId}, #{eventType}, #{toolName}, #{outcome}, #{latencyMs}, #{detail}, NOW())")
    int insert(@Param("userId") Long userId, @Param("sessionId") String sessionId,
               @Param("requestId") String requestId, @Param("eventType") String eventType,
               @Param("toolName") String toolName, @Param("outcome") String outcome,
               @Param("latencyMs") int latencyMs, @Param("detail") String detail);

    @Select("<script>SELECT id, user_id, session_id, request_id, event_type, tool_name, outcome, latency_ms, detail, create_time "
            + "FROM ai_audit_log WHERE 1 = 1 "
            + "<if test='eventType != null and eventType != \"\"'> AND event_type = #{eventType}</if>"
            + "<if test='outcome != null and outcome != \"\"'> AND outcome = #{outcome}</if>"
            + " ORDER BY id DESC LIMIT #{offset}, #{limit}</script>")
    List<AiAuditLog> selectPage(@Param("offset") int offset, @Param("limit") int limit,
                                @Param("eventType") String eventType, @Param("outcome") String outcome);

    @Select("<script>SELECT COUNT(*) FROM ai_audit_log WHERE 1 = 1 "
            + "<if test='eventType != null and eventType != \"\"'> AND event_type = #{eventType}</if>"
            + "<if test='outcome != null and outcome != \"\"'> AND outcome = #{outcome}</if></script>")
    long count(@Param("eventType") String eventType, @Param("outcome") String outcome);
}
