package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_spu")
public class Spu extends BaseEntity {
    private String spuCode;
    private String spuName;
    private Long categoryId;
    private Long brandId;
    /** JSON: {"en":"...","zh":"...","fr":"..."} */
    private String description;
    /** HS Code */
    private String customsCode;
    private String originCountry;
    /** 0草稿 1上架 2下架 */
    private Integer status;
    private Integer salesCount;

    /** C 端商品卡片透传字段，非数据库字段。 */
    @TableField(exist = false)
    private String coverImage;
    @TableField(exist = false)
    private BigDecimal minPrice;
    @TableField(exist = false)
    private String currency;
}
