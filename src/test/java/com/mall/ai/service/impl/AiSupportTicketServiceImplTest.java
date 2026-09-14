package com.mall.ai.service.impl;

import com.mall.ai.entity.AiConversation;
import com.mall.ai.entity.AiSupportTicket;
import com.mall.ai.mapper.AiConversationMapper;
import com.mall.ai.mapper.AiSupportTicketMapper;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSupportTicketServiceImplTest {
    private AiSupportTicketServiceImpl service(AiSupportTicketMapper mapper) {
        return new AiSupportTicketServiceImpl(mock(AiConversationMapper.class), mapper);
    }

    @Test
    void createsTicketForMember() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> { ((AiSupportTicket) invocation.getArgument(0)).setId(1L); return 1; })
                .when(mapper).insert(any(AiSupportTicket.class));
        AiSupportTicketServiceImpl service = service(mapper);

        AiSupportTicket ticket = service.create(9L, "session-1", "支付问题");

        assertThat(ticket.getTicketNo()).startsWith("CS");
        assertThat(ticket.getMemberId()).isEqualTo(9L);
        assertThat(ticket.getStatus()).isZero();
        verify(mapper).insert(any(AiSupportTicket.class));
    }

    @Test
    void reusesOpenTicketForSameConversation() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        AiSupportTicket existing = new AiSupportTicket(); existing.setId(8L); existing.setStatus(0);
        when(mapper.selectOne(any())).thenReturn(existing);

        AiSupportTicket result = service(mapper).create(9L, "session-1", "补充信息");

        assertThat(result.getId()).isEqualTo(8L);
        assertThat(result.getLatestMessage()).isEqualTo("补充信息");
        verify(mapper).updateById(existing);
    }

    @Test
    void claimAndResolveUseCompareAndSetStatus() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        when(mapper.claim(1L, 3L)).thenReturn(1);
        when(mapper.resolve(eq(1L), eq(3L), eq("已处理"))).thenReturn(1);
        AiSupportTicketServiceImpl service = service(mapper);

        service.claim(1L, 3L);
        service.resolve(1L, 3L, "");

        verify(mapper).claim(1L, 3L);
        verify(mapper).resolve(1L, 3L, "已处理");
    }

    @Test
    void loadsConversationLinkedToTicket() {
        AiSupportTicketMapper ticketMapper = mock(AiSupportTicketMapper.class);
        AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
        AiSupportTicket ticket = new AiSupportTicket();
        ticket.setMemberId(9L);
        ticket.setConversationId("session-1");
        AiConversation message = new AiConversation();
        message.setContent("退款进度");
        when(ticketMapper.selectById(8L)).thenReturn(ticket);
        when(conversationMapper.selectRecent(9L, "session-1", 20)).thenReturn(List.of(message));

        List<AiConversation> result = new AiSupportTicketServiceImpl(conversationMapper, ticketMapper).conversation(8L);

        assertThat(result).containsExactly(message);
        verify(conversationMapper).selectRecent(9L, "session-1", 20);
    }

    @Test
    void loadsMemberConversationThroughOwnedTicket() {
        AiSupportTicketMapper ticketMapper = mock(AiSupportTicketMapper.class);
        AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
        AiSupportTicket ticket = new AiSupportTicket();
        ticket.setMemberId(9L);
        ticket.setConversationId("session-1");
        AiConversation message = new AiConversation(); message.setContent("客服回复");
        when(ticketMapper.selectById(8L)).thenReturn(ticket);
        when(conversationMapper.selectRecent(9L, "session-1", 20)).thenReturn(List.of(message));

        List<AiConversation> result = new AiSupportTicketServiceImpl(conversationMapper, ticketMapper)
                .memberConversation(8L, 9L);

        assertThat(result).containsExactly(message);
        verify(conversationMapper).selectRecent(9L, "session-1", 20);
    }

    @Test
    void rejectsMemberConversationForAnotherMember() {
        AiSupportTicketMapper ticketMapper = mock(AiSupportTicketMapper.class);
        AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
        AiSupportTicket ticket = new AiSupportTicket(); ticket.setMemberId(9L);
        when(ticketMapper.selectById(8L)).thenReturn(ticket);

        assertThatThrownBy(() -> new AiSupportTicketServiceImpl(conversationMapper, ticketMapper)
                .memberConversation(8L, 10L)).hasMessageContaining("无权查看");
        verify(conversationMapper, org.mockito.Mockito.never()).selectRecent(any(Long.class), any(String.class), anyInt());
    }

    @Test
    void repliesOnlyThroughAssignedTicket() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        when(mapper.reply(1L, 3L, "已为你查询物流")).thenReturn(1);

        service(mapper).reply(1L, 3L, "  已为你查询物流  ");

        verify(mapper).reply(1L, 3L, "已为你查询物流");
    }

    @Test
    void rejectsReplyWhenTicketIsNotAssignedToAgent() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        when(mapper.reply(1L, 3L, "回复")).thenReturn(0);

        assertThatThrownBy(() -> service(mapper).reply(1L, 3L, "回复"))
                .hasMessageContaining("自己已认领");
    }

    @Test
    void appendsAgentReplyToLinkedConversation() {
        AiSupportTicketMapper ticketMapper = mock(AiSupportTicketMapper.class);
        AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
        AiSupportTicket ticket = new AiSupportTicket();
        ticket.setMemberId(9L);
        ticket.setConversationId("session-1");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketMapper.reply(1L, 3L, "已处理")).thenReturn(1);

        new AiSupportTicketServiceImpl(conversationMapper, ticketMapper).reply(1L, 3L, "已处理");

        ArgumentCaptor<AiConversation> captor = ArgumentCaptor.forClass(AiConversation.class);
        verify(conversationMapper).insert(captor.capture());
        AiConversation message = captor.getValue();
        assertThat(message.getRole()).isEqualTo("agent");
        assertThat(message.getSessionId()).isEqualTo("session-1");
        assertThat(message.getContent()).isEqualTo("已处理");
        assertThat(message.getModel()).isEqualTo("human-agent");
    }

    @Test
    void appendsMemberMessageToLinkedConversation() {
        AiSupportTicketMapper ticketMapper = mock(AiSupportTicketMapper.class);
        AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
        AiSupportTicket ticket = new AiSupportTicket();
        ticket.setMemberId(9L);
        ticket.setConversationId("session-1");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketMapper.memberMessage(1L, 9L, "补充地址")).thenReturn(1);
        when(conversationMapper.insertMemberMessageIfAbsent("session-1", "member-req-1", 9L, "补充地址")).thenReturn(1);

        new AiSupportTicketServiceImpl(conversationMapper, ticketMapper).memberMessage(1L, 9L, "member-req-1", "补充地址");

        verify(conversationMapper).insertMemberMessageIfAbsent("session-1", "member-req-1", 9L, "补充地址");
        verify(ticketMapper).memberMessage(1L, 9L, "补充地址");
    }

    @Test
    void doesNotDuplicateMemberMessageWhenRequestIsRetried() {
        AiSupportTicketMapper ticketMapper = mock(AiSupportTicketMapper.class);
        AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
        AiSupportTicket ticket = new AiSupportTicket();
        ticket.setMemberId(9L);
        ticket.setConversationId("session-1");
        when(ticketMapper.selectById(1L)).thenReturn(ticket);
        when(ticketMapper.memberMessage(1L, 9L, "补充地址")).thenReturn(1);
        when(conversationMapper.insertMemberMessageIfAbsent("session-1", "member-req-1", 9L, "补充地址")).thenReturn(0);

        new AiSupportTicketServiceImpl(conversationMapper, ticketMapper).memberMessage(1L, 9L, "member-req-1", "补充地址");

        verify(conversationMapper).insertMemberMessageIfAbsent("session-1", "member-req-1", 9L, "补充地址");
        verify(ticketMapper, org.mockito.Mockito.never()).memberMessage(1L, 9L, "补充地址");
        verify(conversationMapper, org.mockito.Mockito.never()).insert(any(AiConversation.class));
    }

    @Test
    void rejectsClaimWhenAnotherAgentAlreadyWonRace() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        when(mapper.claim(1L, 3L)).thenReturn(0);

        assertThatThrownBy(() -> service(mapper).claim(1L, 3L))
                .hasMessageContaining("已被其他客服领取");
    }

    @Test
    void rejectsResolveWhenTicketBelongsToAnotherAgent() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        when(mapper.resolve(1L, 3L, "已处理")).thenReturn(0);

        assertThatThrownBy(() -> service(mapper).resolve(1L, 3L, "已处理"))
                .hasMessageContaining("自己已认领");
    }
}
