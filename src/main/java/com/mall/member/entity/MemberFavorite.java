package com.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_favorite")
public class MemberFavorite extends BaseEntity {
    private Long userId;
    private Long spuId;
}
