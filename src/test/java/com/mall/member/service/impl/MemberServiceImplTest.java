package com.mall.member.service.impl;

import com.mall.member.entity.MemberPointsLog;
import com.mall.member.mapper.MemberMapper;
import com.mall.member.mapper.MemberPointsLogMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
}
