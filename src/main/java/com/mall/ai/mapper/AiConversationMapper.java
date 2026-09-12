package com.mall.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.ai.entity.AiConversation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AiConversationMapper extends BaseMapper<AiConversation> {
    @Select("SELECT * FROM (SELECT * FROM ai_conversation WHERE user_id = #{userId} "
            + "AND session_id = #{sessionId} AND deleted = 0 ORDER BY id DESC LIMIT #{limit}) history ORDER BY id")
    List<AiConversation> selectRecent(@Param("userId") Long userId, @Param("sessionId") String sessionId,
                                      @Param("limit") int limit);

    @Select("SELECT * FROM (SELECT * FROM ai_conversation WHERE user_id = #{userId} "
            + "AND session_id = (SELECT session_id FROM ai_conversation "
            + "WHERE user_id = #{userId} AND deleted = 0 ORDER BY id DESC LIMIT 1) "
            + "AND deleted = 0 ORDER BY id DESC LIMIT #{limit}) history ORDER BY id")
    List<AiConversation> selectLatest(@Param("userId") Long userId, @Param("limit") int limit);
}
