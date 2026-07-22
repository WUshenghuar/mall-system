package com.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_cart")
public class TradeCart extends BaseEntity {
    private Long userId;
    private Long skuId;
    private Integer quantity;
    /** 1选中 0未选中 */
    private Integer checked;
}
