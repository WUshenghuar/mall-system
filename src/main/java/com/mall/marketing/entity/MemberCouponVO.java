package com.mall.marketing.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MemberCouponVO {
    private Long issueId;
    private Long couponId;
    private String couponName;
    private String couponType;
    private BigDecimal threshold;
    private BigDecimal discount;
    private String currency;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    /** 0未使用 1已使用 2已过期 */
    private Integer status;
}
