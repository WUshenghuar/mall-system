package com.mall.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.ai.entity.AiSupportTicket;

public interface AiSupportTicketService {
    AiSupportTicket create(Long memberId, String conversationId, String message);
    IPage<AiSupportTicket> selectMemberPage(Long memberId, Integer page, Integer size);
    AiSupportTicket getOwnedById(Long id, Long memberId);
    IPage<AiSupportTicket> selectAdminPage(Integer page, Integer size, Integer status);
    void claim(Long id, Long operatorId);
    void resolve(Long id, String note);
}
