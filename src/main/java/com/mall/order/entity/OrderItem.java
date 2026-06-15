package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("om_order_item")
public class OrderItem extends BaseEntity {
    private Long orderId;
    private Long skuId;
    private Long spuId;
    private String skuName;
    private String skuAttrs;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private BigDecimal tariffRate;
    private BigDecimal tariffAmount;
}
