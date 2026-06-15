package com.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mm_member_points_log")
public class MemberPointsLog {
    private Long id;
    private Long memberId;
    /** 变更积分(正为增加,负为减少) */
    private Integer points;
    private String reason;
    private LocalDateTime createTime;
}
