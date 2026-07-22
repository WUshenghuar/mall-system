package com.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_pay")
public class TradePay extends BaseEntity {
    private String orderNo;
    /** 支付流水号 */
    private String payNo;
    /** 1支付宝 2微信 */
    private Integer payType;
    private BigDecimal payAmount;
    /** 0待支付 1支付成功 2支付失败 */
    private Integer payStatus;
    private LocalDateTime payTime;
    private LocalDateTime callbackTime;
    /** 回调内容 */
    private String callbackContent;
}
