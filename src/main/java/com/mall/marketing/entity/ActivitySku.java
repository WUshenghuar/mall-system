package com.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动SKU实体类
 * 用于存储活动与SKU关联的相关信息，包括秒杀价格、库存等
 */
@Data
@TableName("mk_activity_sku")
public class ActivitySku {
    /** 主键ID */
    private Long id;
    /** 活动ID，关联活动表的主键 */
    private Long activityId;
    /** SKU ID，关联商品SKU表的主键 */
    private Long skuId;
    /** 秒杀价 */
    private BigDecimal seckillPrice;
    /** 秒杀库存 */
    private Integer seckillStock;
    /** 每人限购 */
    private Integer limitPerUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** C 端跳转详情所需的 SPU ID，非数据库字段。 */
    @TableField(exist = false)
    private Long spuId;
}
