package com.mall.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单明细实体类
 * 使用@Data注解自动生成getter、setter等方法
 * @TableName注解指定对应的数据库表名为"fn_statement_item"
 */
@Data
@TableName("fn_statement_item")
public class StatementItem {
    private Long id;                    // 主键ID
    private Long statementId;          // 账单ID
    private String orderNo;            // 订单编号
    private BigDecimal totalAmount;    // 总金额
    private BigDecimal tariffAmount;   // 关税金额
    private BigDecimal shippingFee;    // 运费
    private BigDecimal refundAmount;   // 退款金额
    private BigDecimal payAmount;      // 支付金额
    private LocalDateTime orderTime;   // 下单时间
    private LocalDateTime createTime;  // 创建时间
}
