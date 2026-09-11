package com.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_refund")
public class TradeRefund extends BaseEntity {
    private String orderNo;
    private Long userId;
    private BigDecimal refundAmount;
    private String refundReason;
    /** 0仅退款 1退货退款 */
    private Integer refundType;
    private Integer originalOrderStatus;
    private String evidenceUrls;
    /** 0待审批 1待退货/待退款 2已驳回 3已退款 4待收货 */
    private Integer refundStatus;
    private Long approverId;
    private String approveComment;
    private LocalDateTime approveTime;
    private String returnLogisticsCompany;
    private String returnLogisticsNo;
    private LocalDateTime returnSubmitTime;
}
