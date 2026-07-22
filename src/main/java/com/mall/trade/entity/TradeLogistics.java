package com.mall.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_logistics")
public class TradeLogistics extends BaseEntity {
    private String orderNo;
    /** 物流单号 */
    private String logisticsNo;
    /** 物流公司 */
    private String logisticsCompany;
    /** 物流状态 */
    private Integer logisticsStatus;
    /** 物流轨迹（JSON） */
    private String logisticsInfo;
}
