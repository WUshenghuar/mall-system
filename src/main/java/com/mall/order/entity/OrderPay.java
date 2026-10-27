package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付实体类
 * 对应数据库表 om_order_pay
 */
@Data
@TableName("om_order_pay")
public class OrderPay {
    /** 主键ID */
    private Long id;
    /** 订单ID */
    private Long orderId;
    /** 订单编号 */
    private String orderNo;
    /** 支付单号 */
    private String payNo;
    /** 支付金额 */
    private BigDecimal payAmount;
    /** 货币类型 */
    private String currency;
    /** STRIPE/PAYPAL/ALIPAY */
    private String payType;
    /** 0待支付 1成功 2失败 */
    private Integer payStatus;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
