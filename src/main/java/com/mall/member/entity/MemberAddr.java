package com.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mm_member_addr")
public class MemberAddr extends BaseEntity {
    private Long memberId;
    private String receiverName;
    private String phone;
    private String country;
    private String province;
    private String city;
    private String address;
    private String postcode;
    private Integer isDefault;
}
