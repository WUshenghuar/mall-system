package com.mall.member.service.impl;

import com.mall.member.entity.Member;
import com.mall.member.entity.MemberPointsLog;
import com.mall.member.mapper.MemberMapper;
import com.mall.member.mapper.MemberPointsLogMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberServiceImplTest {
    @Test
    void listPointsLogsReturnsNewestMemberHistory() {
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        MemberPointsLog first = new MemberPointsLog();
        first.setId(2L);
        MemberPointsLog second = new MemberPointsLog();
        second.setId(1L);
        when(logMapper.selectList(any())).thenReturn(List.of(first, second));

        MemberServiceImpl service = new MemberServiceImpl(mock(MemberMapper.class), logMapper);

        assertThat(service.listPointsLogs(9L)).containsExactly(first, second);
    }

    @Test
    void completedOrderAccumulatesSpendingPointsAndLevel() {
        MemberMapper memberMapper = mock(MemberMapper.class);
        MemberPointsLogMapper logMapper = mock(MemberPointsLogMapper.class);
        Member member = new Member();
        member.setId(9L);
        member.setPoints(12);
        member.setTotalAmount(new BigDecimal("900.00"));
        member.setLevel(0);
        when(memberMapper.selectById(9L)).thenReturn(member);

        MemberServiceImpl service = new MemberServiceImpl(memberMapper, logMapper);
        service.recordOrderCompletion(9L, new BigDecimal("120.50"), "T-1");

        assertThat(member.getTotalAmount()).isEqualByComparingTo("1020.50");
        assertThat(member.getPoints()).isEqualTo(132);
        assertThat(member.getLevel()).isEqualTo(1);
        verify(logMapper).insert(org.mockito.ArgumentMatchers.argThat((MemberPointsLog log) ->
                log.getMemberId().equals(9L) && log.getPoints() == 120 && log.getReason().contains("T-1")));
    }
}
