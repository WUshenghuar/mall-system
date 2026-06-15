package com.mall.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fn_statement_item")
public class StatementItem {
    private Long id;
    private Long statementId;
    private String orderNo;
    private BigDecimal totalAmount;
    private BigDecimal tariffAmount;
    private BigDecimal shippingFee;
    private BigDecimal refundAmount;
    private BigDecimal payAmount;
    private LocalDateTime orderTime;
    private LocalDateTime createTime;
}
