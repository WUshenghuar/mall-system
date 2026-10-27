package com.mall.finance.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账单导出视图对象
 * 用于封装导出对账单所需的数据字段
 * 包含订单基本信息、金额信息以及时间信息
 */
@Data
public class StatementExportVO {
    /**
     * 订单号
     * 用于唯一标识一笔交易
     */
    @ExcelProperty("订单号")
    private String orderNo;

    /**
     * 交易金额(USD)
     * 表示订单的总交易金额，单位为美元
     */
    @ExcelProperty("交易金额(USD)")
    private BigDecimal totalAmount;

    /**
     * 关税(USD)
     * 表示订单需要支付的关税金额，单位为美元
     */
    @ExcelProperty("关税(USD)")
    private BigDecimal tariffAmount;

    /**
     * 运费(USD)
     * 表示订单的运输费用，单位为美元
     */
    @ExcelProperty("运费(USD)")
    private BigDecimal shippingFee;

    /**
     * 退款金额(USD)
     * 表示订单的退款金额，单位为美元
     */
    @ExcelProperty("退款金额(USD)")
    private BigDecimal refundAmount;

    /**
     * 实付金额(USD)
     * 表示订单的实际支付金额，单位为美元
     */
    @ExcelProperty("实付(USD)")
    private BigDecimal payAmount;

    /**
     * 下单时间
     * 表示订单创建的具体时间
     */
    @ExcelProperty("下单时间")
    private LocalDateTime createTime;
}
