package com.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mk_coupon_issue")
public class CouponIssue {
    private Long id;
    private Long couponId;
    private Long memberId;
    private LocalDateTime issueTime;
    private LocalDateTime usedTime;
    /** 0未使用 1已使用 2已过期 */
    private Integer status;
    private String orderNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
