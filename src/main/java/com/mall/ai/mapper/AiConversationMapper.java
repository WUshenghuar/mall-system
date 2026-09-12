package com.mall.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.ai.entity.AiConversation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

public interface AiConversationMapper extends BaseMapper<AiConversation> {
    @Select("SELECT * FROM ai_conversation WHERE user_id = #{userId} AND session_id = #{sessionId} "
            + "AND request_id = #{requestId} AND role = 'assistant' AND deleted = 0 ORDER BY id DESC LIMIT 1")
    AiConversation selectAssistantByRequest(@Param("userId") Long userId, @Param("sessionId") String sessionId,
                                            @Param("requestId") String requestId);

    @Insert("INSERT IGNORE INTO ai_conversation "
            + "(session_id, request_id, user_id, role, content, tokens_used, latency_ms, model, create_time, update_time, deleted) "
            + "VALUES (#{sessionId}, #{requestId}, #{userId}, 'user', #{content}, 0, 0, 'customer-service-p0', NOW(), NOW(), 0)")
    int insertUserIfAbsent(@Param("sessionId") String sessionId, @Param("requestId") String requestId,
                           @Param("userId") Long userId, @Param("content") String content);

    @Update("UPDATE ai_conversation SET request_id = NULL, deleted = 1, update_time = NOW() "
            + "WHERE user_id = #{userId} AND session_id = #{sessionId} AND request_id = #{requestId} "
            + "AND role = 'user' AND deleted = 0")
    int discardUserRequest(@Param("userId") Long userId, @Param("sessionId") String sessionId,
                           @Param("requestId") String requestId);

    @Select("SELECT * FROM (SELECT * FROM ai_conversation WHERE user_id = #{userId} "
            + "AND session_id = #{sessionId} AND deleted = 0 ORDER BY id DESC LIMIT #{limit}) history ORDER BY id")
    List<AiConversation> selectRecent(@Param("userId") Long userId, @Param("sessionId") String sessionId,
                                      @Param("limit") int limit);

    @Select("SELECT * FROM (SELECT * FROM ai_conversation WHERE user_id = #{userId} "
            + "AND session_id = (SELECT session_id FROM ai_conversation "
            + "WHERE user_id = #{userId} AND deleted = 0 ORDER BY id DESC LIMIT 1) "
            + "AND deleted = 0 ORDER BY id DESC LIMIT #{limit}) history ORDER BY id")
    List<AiConversation> selectLatest(@Param("userId") Long userId, @Param("limit") int limit);

    @Update("UPDATE ai_conversation SET feedback = #{feedback}, update_time = NOW() "
            + "WHERE id = #{messageId} AND user_id = #{userId} AND role = 'assistant' AND deleted = 0")
    int updateFeedback(@Param("messageId") Long messageId, @Param("userId") Long userId,
                       @Param("feedback") Integer feedback);

    @Select("SELECT COUNT(*) AS total, "
            + "COALESCE(SUM(CASE WHEN feedback = 1 THEN 1 ELSE 0 END), 0) AS positive, "
            + "COALESCE(SUM(CASE WHEN feedback = -1 THEN 1 ELSE 0 END), 0) AS negative "
            + "FROM ai_conversation WHERE role = 'assistant' AND feedback IS NOT NULL AND deleted = 0")
    Map<String, Object> selectFeedbackStats();
}
