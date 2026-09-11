package com.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_support_ticket")
public class AiSupportTicket extends BaseEntity {
    private String ticketNo;
    private Long memberId;
    private String conversationId;
    private String subject;
    private String latestMessage;
    /** 0待接管 1处理中 2已解决 */
    private Integer status;
    private Long assignedUserId;
    private String handledNote;
}
