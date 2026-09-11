package com.mall.web.controller.system;

import com.mall.member.mapper.MemberMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.mapper.TradeRefundMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardControllerTest {
    @Test
    void todayStatsUseTradeOrdersAndTradeRefunds() {
        SpuMapper spuMapper = mock(SpuMapper.class);
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        MemberMapper memberMapper = mock(MemberMapper.class);
        TradeRefundMapper refundMapper = mock(TradeRefundMapper.class);
        when(spuMapper.selectCount(null)).thenReturn(8L);
        when(memberMapper.selectCount(null)).thenReturn(3L);
        when(orderMapper.selectCount(any())).thenReturn(5L);
        when(refundMapper.selectCount(any())).thenReturn(2L);
        when(orderMapper.countByHourSince(any(LocalDateTime.class)))
                .thenReturn(List.of(Map.of("hourKey", LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH")), "count", 3L)));

        DashboardController controller = new DashboardController(spuMapper, orderMapper, memberMapper, refundMapper);

        Map<String, Object> stats = controller.stats("today").getData();

        assertThat(stats).containsEntry("todayOrders", 5L).containsEntry("pendingRefund", 2L);
        assertThat((List<Long>) stats.get("hourlyOrderCounts")).hasSize(24).contains(3L);
    }

    @Test
    void twentyFourHourStatsExposeSelectedRangeAndTwentyFourBuckets() {
        SpuMapper spuMapper = mock(SpuMapper.class);
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        MemberMapper memberMapper = mock(MemberMapper.class);
        TradeRefundMapper refundMapper = mock(TradeRefundMapper.class);
        when(orderMapper.countByHourSince(any(LocalDateTime.class))).thenReturn(List.of());

        DashboardController controller = new DashboardController(spuMapper, orderMapper, memberMapper, refundMapper);

        Map<String, Object> stats = controller.stats("24h").getData();

        assertThat(stats).containsEntry("range", "24h");
        assertThat((List<String>) stats.get("hourLabels")).hasSize(24);
        assertThat((List<Long>) stats.get("hourlyOrderCounts")).hasSize(24);
    }
}
