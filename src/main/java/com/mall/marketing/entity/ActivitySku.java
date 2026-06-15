package com.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mk_activity_sku")
public class ActivitySku {
    private Long id;
    private Long activityId;
    private Long skuId;
    /** 秒杀价 */
    private BigDecimal seckillPrice;
    /** 秒杀库存 */
    private Integer seckillStock;
    /** 每人限购 */
    private Integer limitPerUser;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
