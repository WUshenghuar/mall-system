package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_sku")
public class Sku extends BaseEntity {
    private Long spuId;
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
}
