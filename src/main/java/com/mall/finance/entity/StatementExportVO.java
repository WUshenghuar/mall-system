package com.mall.finance.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StatementExportVO {
    @ExcelProperty("订单号")
    private String orderNo;

    @ExcelProperty("交易金额(USD)")
    private BigDecimal totalAmount;

    @ExcelProperty("关税(USD)")
    private BigDecimal tariffAmount;

    @ExcelProperty("运费(USD)")
    private BigDecimal shippingFee;

    @ExcelProperty("退款金额(USD)")
    private BigDecimal refundAmount;

    @ExcelProperty("实付(USD)")
    private BigDecimal payAmount;

    @ExcelProperty("下单时间")
    private LocalDateTime createTime;
}
