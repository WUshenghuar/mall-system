package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * SKU（Stock Keeping Unit）实体类，继承自BaseEntity
 * 用于存储商品的具体规格信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_sku")
public class Sku extends BaseEntity {
    /** 商品SPU ID */
    private Long spuId;
    /** SKU编码 */
    private String skuCode;
    /** JSON: [{"k":"颜色","v":"黑色"},{"k":"容量","v":"128GB"}] */
    private String attrs;
    /** USD */
    private BigDecimal price;
    private String currency;
    private BigDecimal costPrice;
    /** kg */
    private BigDecimal weight;
    /** JSON 图片列表 */
    private String images;
    private Integer status;

    /** 库存（透传，非表字段） */
    @TableField(exist = false)
    private Integer stock;

    /** 图片预览地址（透传，非表字段） */
    @TableField(exist = false)
    private String imageUrl;
}
