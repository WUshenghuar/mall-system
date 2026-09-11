package com.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_order_item")
public class TradeOrderItem extends BaseEntity {
    private Long orderId;
    private String orderNo;
    private Long skuId;
    /** 活动 ID 快照，普通订单为空 */
    private Long activityId;
    /** 是否实际占用活动库存 */
    private Integer activityStockReserved;
    /** 商品名称（快照） */
    private String skuName;
    /** 商品单价（快照） */
    private BigDecimal skuPrice;
    private Integer quantity;
    /** 小计 */
    private BigDecimal totalAmount;
    /** 商品图片（快照） */
    private String skuImage;
}
