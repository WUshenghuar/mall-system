package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("om_order_logistics")
public class OrderLogistics extends BaseEntity {
    private Long orderId;
    private String trackingNo;
    /** DHL/FedEx/UPS/EMS */
    private String carrier;
    /** 报关单号 */
    private String customsDeclareNo;
    /** 0未报关 1已报关 2清关中 3已清关 */
    private Integer customsStatus;
    /** JSON: [{"time":"...","location":"...","desc":"..."}] */
    private String trackingEvents;
    private String originCountry;
    private String destCountry;
}
