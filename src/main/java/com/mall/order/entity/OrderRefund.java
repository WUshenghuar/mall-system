package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("om_order_refund")
public class OrderRefund extends BaseEntity {
    private Long orderId;
    private String orderNo;
    private Long skuId;
    private Integer quantity;
    private BigDecimal refundAmount;
    private String currency;
    private String refundReason;
    /** 0仅退款 1退货退款 */
    private Integer refundType;
    /** 0待审批 1已通过 2已驳回 3已完成 */
    private Integer refundStatus;
    private Long applicantId;
    private Long approverId;
    private LocalDateTime approveTime;
    private String approveComment;
}
