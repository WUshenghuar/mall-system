package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pm_sku_stock")
public class SkuStock {
    private Long id;
    private Long skuId;
    private Long warehouseId;
    private Integer stock;
    /** 锁定库存（下单未支付） */
    private Integer lockedStock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
