package com.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_account")
public class MemberAccount extends BaseEntity {
    private Long memberId;
    private String phone;
    private String passwordHash;
    private Integer status;
}
