package com.mall.marketing.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员优惠券值对象(VO)
 * 用于展示会员优惠券相关信息
 */
@Data
public class MemberCouponVO {
    // 优惠券发放ID
    private Long issueId;
    // 优惠券ID
    private Long couponId;
    // 优惠券名称
    private String couponName;
    // 优惠券类型
    private String couponType;
    // 使用门槛金额
    private BigDecimal threshold;
    // 优惠金额或折扣率
    private BigDecimal discount;
    // 货币单位
    private String currency;
    // 优惠券生效开始时间
    private LocalDateTime validStart;
    // 优惠券生效结束时间
    private LocalDateTime validEnd;
    /** 0未使用 1已使用 2已过期 */
    private Integer status;
}
