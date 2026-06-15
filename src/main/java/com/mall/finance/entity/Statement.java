package com.mall.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fn_statement")
public class Statement extends BaseEntity {
    private String statementNo;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalAmount;
    private BigDecimal tariffAmount;
    private BigDecimal shippingFee;
    private BigDecimal refundAmount;
    private BigDecimal netAmount;
    private Integer orderCount;
    /** 0待确认 1已确认 */
    private Integer status;
}
