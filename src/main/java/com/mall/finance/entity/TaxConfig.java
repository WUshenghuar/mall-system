package com.mall.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fn_tax_config")
public class TaxConfig extends BaseEntity {
    private Long categoryId;
    private String originCountry;
    private String destCountry;
    private BigDecimal taxRate;
    /** DUTY/VAT/SALES_TAX */
    private String taxType;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
}
