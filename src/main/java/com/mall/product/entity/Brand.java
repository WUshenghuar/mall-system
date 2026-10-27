package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 品牌实体类
 * 继承自BaseEntity，包含品牌的基本信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_brand")
public class Brand extends BaseEntity {
    private String brandName;    // 品牌名称
    private String brandLogo;    // 品牌Logo图片地址
    private String brandDesc;    // 品牌描述
    private Integer orderNum;    // 排序序号
    private Integer status;       // 品牌状态
}
