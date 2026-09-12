package com.mall.ai.service.impl;

import com.mall.ai.entity.AiSupportTicket;
import com.mall.ai.mapper.AiSupportTicketMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSupportTicketServiceImplTest {
    @Test
    void createsTicketForMember() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> { ((AiSupportTicket) invocation.getArgument(0)).setId(1L); return 1; })
                .when(mapper).insert(any(AiSupportTicket.class));
        AiSupportTicketServiceImpl service = new AiSupportTicketServiceImpl(mapper);

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

        AiSupportTicket result = new AiSupportTicketServiceImpl(mapper).create(9L, "session-1", "补充信息");

        assertThat(result.getId()).isEqualTo(8L);
        assertThat(result.getLatestMessage()).isEqualTo("补充信息");
        verify(mapper).updateById(existing);
    }

    @Test
    void claimAndResolveUseCompareAndSetStatus() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        when(mapper.claim(1L, 3L)).thenReturn(1);
        when(mapper.resolve(eq(1L), eq("已处理"))).thenReturn(1);
        AiSupportTicketServiceImpl service = new AiSupportTicketServiceImpl(mapper);

        service.claim(1L, 3L);
        service.resolve(1L, "");

        verify(mapper).claim(1L, 3L);
        verify(mapper).resolve(1L, "已处理");
    }

    @Test
    void repliesOnlyThroughAssignedTicket() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        when(mapper.reply(1L, 3L, "已为你查询物流")).thenReturn(1);

        new AiSupportTicketServiceImpl(mapper).reply(1L, 3L, "  已为你查询物流  ");

        verify(mapper).reply(1L, 3L, "已为你查询物流");
    }

    @Test
    void rejectsReplyWhenTicketIsNotAssignedToAgent() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        when(mapper.reply(1L, 3L, "回复")).thenReturn(0);

        assertThatThrownBy(() -> new AiSupportTicketServiceImpl(mapper).reply(1L, 3L, "回复"))
                .hasMessageContaining("自己已认领");
    }

    @Test
    void rejectsClaimWhenAnotherAgentAlreadyWonRace() {
        AiSupportTicketMapper mapper = mock(AiSupportTicketMapper.class);
        when(mapper.claim(1L, 3L)).thenReturn(0);

        assertThatThrownBy(() -> new AiSupportTicketServiceImpl(mapper).claim(1L, 3L))
                .hasMessageContaining("已被其他客服领取");
    }
}
