package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("om_order")
public class Order extends BaseEntity {
    private String orderNo;
    private Long userId;
    private Long memberId;
    private BigDecimal totalAmount;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal customsDeclare;
    private BigDecimal tariffAmount;
    private BigDecimal tariffRate;
    private BigDecimal shippingFee;
    private String shippingMethod;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    /** 0待支付 1已支付 2已发货 3已签收 4已完成 5已取消 */
    private Integer orderStatus;
    /** 0未支付 1已支付 2退款中 3已退款 */
    private Integer payStatus;
    /** 0未发货 1已出关 2运输中 3已入关 4已签收 */
    private Integer logisticsStatus;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
}
