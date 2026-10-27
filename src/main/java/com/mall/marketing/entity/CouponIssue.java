package com.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券发放实体类
 * 使用@Data注解自动生成getter、setter、toString等方法
 * 使用@TableName注解指定对应的数据库表名为"mk_coupon_issue"
 */
@Data
@TableName("mk_coupon_issue")
public class CouponIssue {
    private Long id;                // 主键ID
    private Long couponId;          // 优惠券ID
    private Long memberId;          // 会员ID
    private LocalDateTime issueTime;    // 发放时间
    private LocalDateTime usedTime;     // 使用时间
    /** 0未使用 1已使用 2已过期 */
    private Integer status;         // 优惠券状态：0未使用 1已使用 2已过期
    private String orderNo;         // 订单编号
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间
}
