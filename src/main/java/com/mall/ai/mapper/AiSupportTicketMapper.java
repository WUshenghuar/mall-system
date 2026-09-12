package com.mall.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.ai.entity.AiSupportTicket;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface AiSupportTicketMapper extends BaseMapper<AiSupportTicket> {
    @Update("UPDATE ai_support_ticket SET status = 1, assigned_user_id = #{operatorId}, update_time = NOW() "
            + "WHERE id = #{id} AND status = 0 AND deleted = 0")
    int claim(@Param("id") Long id, @Param("operatorId") Long operatorId);

    @Update("UPDATE ai_support_ticket SET agent_reply = #{reply}, update_time = NOW() "
            + "WHERE id = #{id} AND status = 1 AND assigned_user_id = #{operatorId} AND deleted = 0")
    int reply(@Param("id") Long id, @Param("operatorId") Long operatorId, @Param("reply") String reply);

    @Update("UPDATE ai_support_ticket SET status = 2, handled_note = #{note}, update_time = NOW() "
            + "WHERE id = #{id} AND status = 1 AND deleted = 0")
    int resolve(@Param("id") Long id, @Param("note") String note);
}
