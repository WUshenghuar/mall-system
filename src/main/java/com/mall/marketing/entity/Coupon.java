package com.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mk_coupon")
public class Coupon extends BaseEntity {
    private String couponName;
    /** FULL_REDUCTION / DISCOUNT / SHIPPING */
    private String couponType;
    /** 满减门槛 */
    private BigDecimal threshold;
    /** 减免金额/折扣率 */
    private BigDecimal discount;
    private String currency;
    /** 发行总量 */
    private Integer maxIssue;
    /** 已发行 */
    private Integer issuedCount;
    /** 每人限领 */
    private Integer perLimit;
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    /** ALL / CATEGORY / SKU */
    private String scope;
    /** 适用范围ID（逗号分隔） */
    private String scopeIds;
    /** 0草稿 1待审核 2已发布 3已结束 */
    private Integer status;
}
