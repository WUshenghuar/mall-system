package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_category")
public class Category extends BaseEntity {
    private String categoryName;
    private Long parentId;
    private Integer level;
    private String icon;
    private Integer orderNum;
    private Integer status;
}
