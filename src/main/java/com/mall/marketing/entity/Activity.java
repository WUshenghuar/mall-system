package com.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mk_activity")
public class Activity extends BaseEntity {
    private String activityName;
    /** SECKILL / DISCOUNT / FULL_REDUCTION */
    private String activityType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 0未开始 1进行中 2已结束 */
    private Integer status;
    @TableField(exist = false)
    private List<ActivitySku> skuItems;
}
