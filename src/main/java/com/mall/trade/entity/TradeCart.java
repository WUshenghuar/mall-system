package com.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 购物车实体类
 * 继承自BaseEntity，包含购物车的基本信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_cart")
public class TradeCart extends BaseEntity {
    /**
     * 用户ID，关联用户表的主键
     */
    private Long userId;
    /**
     * 商品SKU ID，关联商品SKU表的主键
     */
    private Long skuId;
    /**
     * 商品数量
     */
    private Integer quantity;
    /** 1选中 0未选中 */
    private Integer checked;
}
