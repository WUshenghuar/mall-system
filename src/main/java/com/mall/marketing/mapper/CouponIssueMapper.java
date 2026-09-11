package com.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.marketing.entity.CouponIssue;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CouponIssueMapper extends BaseMapper<CouponIssue> {
    @Update("UPDATE mk_coupon_issue SET status = 1, used_time = NOW(), order_no = #{orderNo}, update_time = NOW() "
            + "WHERE id = #{issueId} AND coupon_id = #{couponId} AND member_id = #{memberId} AND status = 0")
    int markUsed(@Param("issueId") Long issueId, @Param("couponId") Long couponId,
                 @Param("memberId") Long memberId, @Param("orderNo") String orderNo);
}
