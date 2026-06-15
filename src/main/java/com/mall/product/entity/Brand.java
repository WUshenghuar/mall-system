package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_brand")
public class Brand extends BaseEntity {
    private String brandName;
    private String brandLogo;
    private String brandDesc;
    private Integer orderNum;
    private Integer status;
}
