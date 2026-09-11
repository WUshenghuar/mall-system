package com.mall.ai.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.ai.entity.AiSupportTicket;
import com.mall.ai.mapper.AiSupportTicketMapper;
import com.mall.ai.service.AiSupportTicketService;
import com.mall.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiSupportTicketServiceImpl implements AiSupportTicketService {
    private final AiSupportTicketMapper ticketMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSupportTicket create(Long memberId, String conversationId, String message) {
        String latestMessage = StringUtils.hasText(message) ? message.trim() : "需要人工客服协助";
        String sessionId = StringUtils.hasText(conversationId) ? conversationId.trim() : null;
        if (sessionId != null) {
            AiSupportTicket existing = ticketMapper.selectOne(Wrappers.<AiSupportTicket>lambdaQuery()
                    .eq(AiSupportTicket::getMemberId, memberId).eq(AiSupportTicket::getConversationId, sessionId)
                    .in(AiSupportTicket::getStatus, 0, 1).last("LIMIT 1"));
            if (existing != null) {
                existing.setLatestMessage(latestMessage);
                ticketMapper.updateById(existing);
                return existing;
            }
        }
        AiSupportTicket ticket = new AiSupportTicket();
        ticket.setTicketNo("CS" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        ticket.setMemberId(memberId); ticket.setConversationId(sessionId);
        ticket.setSubject(latestMessage.substring(0, Math.min(latestMessage.length(), 60)));
        ticket.setLatestMessage(latestMessage); ticket.setStatus(0);
        ticketMapper.insert(ticket);
        return ticket;
    }

    @Override
    public IPage<AiSupportTicket> selectMemberPage(Long memberId, Integer page, Integer size) {
        return ticketMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50)),
                Wrappers.<AiSupportTicket>lambdaQuery().eq(AiSupportTicket::getMemberId, memberId)
                        .orderByDesc(AiSupportTicket::getCreateTime));
    }

    @Override
    public AiSupportTicket getOwnedById(Long id, Long memberId) {
        AiSupportTicket ticket = ticketMapper.selectById(id);
        if (ticket == null || !memberId.equals(ticket.getMemberId())) throw new BusinessException("工单不存在或无权查看");
        return ticket;
    }

    @Override
    public IPage<AiSupportTicket> selectAdminPage(Integer page, Integer size, Integer status) {
        return ticketMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100)),
                Wrappers.<AiSupportTicket>lambdaQuery().eq(status != null, AiSupportTicket::getStatus, status)
                        .orderByAsc(AiSupportTicket::getStatus).orderByDesc(AiSupportTicket::getCreateTime));
    }

    @Override
    @Transactional
    public void claim(Long id, Long operatorId) {
        if (ticketMapper.claim(id, operatorId) != 1) throw new BusinessException("工单已被其他客服领取或已关闭");
    }

    @Override
    @Transactional
    public void resolve(Long id, String note) {
        if (ticketMapper.resolve(id, StringUtils.hasText(note) ? note.trim() : "已处理") != 1) {
            throw new BusinessException("工单状态已变更，请刷新后重试");
        }
    }
}
