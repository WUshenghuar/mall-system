package com.mall.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 税务配置实体类
 * 对应数据库表 fn_tax_config，用于存储跨境交易中不同国家、不同商品分类的税率配置规则。
 * 继承 BaseEntity 以获取公共字段（如 id, create_time, update_time 等）。
 */
@Data
@EqualsAndHashCode(callSuper = true) // 纳入父类 BaseEntity 的字段共同计算 equals 和 hashCode
@TableName("fn_tax_config")
public class TaxConfig extends BaseEntity {

    /**
     * 商品分类ID
     * 关联商品分类表，用于针对不同类目设置差异化税率（如奢侈品与日用品税率不同）
     */
    private Long categoryId;

    /**
     * 原产国/起运国编码
     * 遵循 ISO 3166-1 alpha 2 标准国家代码（如：CN 代表中国）
     */
    private String originCountry;

    /**
     * 目的国编码
     * 遵循 ISO 3166-1 alpha 2 标准国家代码（如：US 代表美国）
     */
    private String destCountry;

    /**
     * 税率
     * 使用 BigDecimal 保证金融计算精度，避免浮点数丢失精度。
     * 示例：若税率为 13%，则存储 0.13
     */
    private BigDecimal taxRate;

    /**
     * 税种类型
     * DUTY: 关税
     * VAT: 增值税
     * SALES_TAX: 销售税
     */
    private String taxType;

    /**
     * 生效日期
     * 税率生效的起始时间（精确到天），用于支持未来税率的提前配置与自动切换
     */
    private LocalDate effectiveDate;

    /**
     * 失效日期
     * 税率生效的截止时间（精确到天），用于控制税率的有效周期，过期规则不再参与计费
     */
    private LocalDate expireDate;
}
