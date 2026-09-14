package com.mall.ai.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.ai.entity.AiConversation;
import com.mall.ai.entity.AiSupportTicket;
import com.mall.ai.mapper.AiConversationMapper;
import com.mall.ai.mapper.AiSupportTicketMapper;
import com.mall.ai.service.AiSupportTicketService;
import com.mall.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiSupportTicketServiceImpl implements AiSupportTicketService {
    private final AiConversationMapper conversationMapper;
    private final AiSupportTicketMapper ticketMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiSupportTicket create(Long memberId, String conversationId, String message) {
        String latestMessage = StringUtils.hasText(message) ? message.trim() : "需要人工客服协助";
        String sessionId = StringUtils.hasText(conversationId) ? conversationId.trim() : null;
        if (sessionId != null) {
            AiSupportTicket existing = ticketMapper.selectOne(Wrappers.<AiSupportTicket>lambdaQuery()
                    .eq(AiSupportTicket::getMemberId, memberId).eq(AiSupportTicket::getConversationId, sessionId)
                    .in(AiSupportTicket::getStatus, 0, 1).last("LIMIT 1 FOR UPDATE"));
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
    public List<AiConversation> conversation(Long ticketId) {
        AiSupportTicket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) throw new BusinessException("工单不存在");
        if (!StringUtils.hasText(ticket.getConversationId())) return List.of();
        return conversationMapper.selectRecent(ticket.getMemberId(), ticket.getConversationId(), 20);
    }

    @Override
    @Transactional
    public void claim(Long id, Long operatorId) {
        if (ticketMapper.claim(id, operatorId) != 1) throw new BusinessException("工单已被其他客服领取或已关闭");
    }

    @Override
    @Transactional
    public void reply(Long id, Long operatorId, String reply) {
        String content = StringUtils.hasText(reply) ? reply.trim() : "已收到你的问题，客服正在处理中。";
        AiSupportTicket ticket = ticketMapper.selectById(id);
        if (ticketMapper.reply(id, operatorId, content) != 1) {
            throw new BusinessException("只能回复自己已认领的处理中工单");
        }
        if (ticket != null && StringUtils.hasText(ticket.getConversationId())) {
            AiConversation message = new AiConversation();
            message.setSessionId(ticket.getConversationId());
            message.setUserId(ticket.getMemberId());
            message.setRole("agent");
            message.setContent(content);
            message.setModel("human-agent");
            conversationMapper.insert(message);
        }
    }

    @Override
    @Transactional
    public void memberMessage(Long id, Long memberId, String requestId, String message) {
        String content = StringUtils.hasText(message) ? message.trim() : "需要补充说明";
        String idempotencyKey = StringUtils.hasText(requestId) ? requestId.trim() : null;
        AiSupportTicket ticket = ticketMapper.selectById(id);
        if (ticket == null || !memberId.equals(ticket.getMemberId())) {
            throw new BusinessException("工单不存在或无权操作");
        }
        if (ticketMapper.memberMessage(id, memberId, content) != 1) {
            throw new BusinessException("只有处理中工单可以补充留言");
        }
        if (StringUtils.hasText(ticket.getConversationId())) {
            conversationMapper.insertMemberMessageIfAbsent(ticket.getConversationId(), idempotencyKey, memberId, content);
        }
    }

    @Override
    @Transactional
    public void resolve(Long id, Long operatorId, String note) {
        if (ticketMapper.resolve(id, operatorId, StringUtils.hasText(note) ? note.trim() : "已处理") != 1) {
            throw new BusinessException("只能解决自己已认领的处理中工单");
        }
    }
}
