package com.mall.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.trade.entity.TradeRefund;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface TradeRefundMapper extends BaseMapper<TradeRefund> {
    @Update("UPDATE trade_refund SET refund_status = #{targetStatus}, approver_id = #{approverId}, "
            + "approve_comment = #{comment}, approve_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND refund_status = #{expectedStatus}")
    int transitionStatus(@Param("id") Long id, @Param("expectedStatus") int expectedStatus,
                         @Param("targetStatus") int targetStatus, @Param("approverId") Long approverId,
                         @Param("comment") String comment);
}
