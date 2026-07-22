package com.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_order")
public class TradeOrder extends BaseEntity {
    private String orderNo;
    private Long userId;
    /** 0待支付 1待发货 2待收货 3已完成 4已取消 5退款中 6已退款 */
    private Integer orderStatus;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private BigDecimal payAmount;
    /** 1支付宝 2微信 */
    private Integer payType;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    /** 1APP 2H5 3小程序 */
    private Integer sourceType;
}
