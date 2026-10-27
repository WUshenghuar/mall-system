package com.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员积分日志实体类
 * 使用@Data注解自动生成getter、setter、toString等方法
 * @TableName注解指定对应的数据库表名为"mm_member_points_log"
 */
@Data
@TableName("mm_member_points_log")
public class MemberPointsLog {
    private Long id;                    // 主键ID
    private Long memberId;              // 会员ID
    /** 变更积分(正为增加,负为减少) */
    private Integer points;             // 积分变更值
    private String reason;              // 积分变更原因
    private LocalDateTime createTime;    // 创建时间
}
