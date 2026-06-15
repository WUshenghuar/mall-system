package com.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mm_member")
public class Member extends BaseEntity {
    private String email;
    private String phone;
    private String password;
    private String nickName;
    private Integer gender;
    private String avatar;
    /** 0普通 1Gold 2Platinum */
    private Integer level;
    /** 积分 */
    private Integer points;
    /** 累计消费 */
    private BigDecimal totalAmount;
    private Integer status;
}
