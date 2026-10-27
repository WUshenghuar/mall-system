package com.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Activity类，表示活动实体
 * 继承自BaseEntity类，使用Lombok注解简化代码
 * 对应数据库表名为"mk_activity"
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mk_activity")
/**
 * Activity类，继承自BaseEntity
 * 用于存储活动相关信息
 */
public class Activity extends BaseEntity {
    private String activityName;  // 活动名称
    /** SECKILL / DISCOUNT / FULL_REDUCTION */
    private String activityType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 0未开始 1进行中 2已结束 */
    private Integer status;
    @TableField(exist = false)
    private List<ActivitySku> skuItems;
}
