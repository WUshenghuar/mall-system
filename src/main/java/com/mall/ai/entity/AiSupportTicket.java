package com.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AiSupportTicket类 - AI支持工单实体类
 * 继承自BaseEntity，包含工单的基本信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_support_ticket")
public class AiSupportTicket extends BaseEntity {
    private String ticketNo;        // 工单编号
    private Long memberId;          // 会员ID
    private String conversationId;  // 对话ID
    private String subject;         // 工单主题
    private String latestMessage;   // 最新消息内容
    private String agentReply;      // 客服回复内容
    /** 0待接管 1处理中 2已解决 */
    private Integer status;         // 工单状态：0-待接管，1-处理中，2-已解决
    private Long assignedUserId;     // 分配的客服用户ID
    private String handledNote;     // 处理备注
}
