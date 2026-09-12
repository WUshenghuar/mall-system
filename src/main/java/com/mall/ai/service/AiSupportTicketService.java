package com.mall.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.ai.entity.AiConversation;
import com.mall.ai.entity.AiSupportTicket;

import java.util.List;

public interface AiSupportTicketService {
    AiSupportTicket create(Long memberId, String conversationId, String message);
    IPage<AiSupportTicket> selectMemberPage(Long memberId, Integer page, Integer size);
    AiSupportTicket getOwnedById(Long id, Long memberId);
    IPage<AiSupportTicket> selectAdminPage(Integer page, Integer size, Integer status);
    List<AiConversation> conversation(Long ticketId);
    void claim(Long id, Long operatorId);
    void reply(Long id, Long operatorId, String reply);
    void resolve(Long id, String note);
}
