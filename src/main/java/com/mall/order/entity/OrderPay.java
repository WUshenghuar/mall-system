package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("om_order_pay")
public class OrderPay {
    private Long id;
    private Long orderId;
    private String orderNo;
    private String payNo;
    private BigDecimal payAmount;
    private String currency;
    /** STRIPE/PAYPAL/ALIPAY */
    private String payType;
    /** 0待支付 1成功 2失败 */
    private Integer payStatus;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
