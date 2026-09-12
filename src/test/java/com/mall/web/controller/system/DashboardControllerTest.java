package com.mall.web.controller.system;

import com.mall.member.mapper.MemberMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.security.user.LoginUser;
import com.mall.security.user.MemberPrincipal;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.mapper.TradeRefundMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        when(orderMapper.sumPaidAmountSince(any(LocalDateTime.class))).thenReturn(new BigDecimal("199.90"));
        when(refundMapper.selectCount(any())).thenReturn(2L);
        when(orderMapper.countByHourSince(any(LocalDateTime.class)))
                .thenReturn(List.of(Map.of("hourKey", LocalDateTime.now().withMinute(0).withSecond(0).withNano(0)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH")), "count", 3L)));
        Authentication staff = mock(Authentication.class);
        when(staff.getPrincipal()).thenReturn(mock(LoginUser.class));

        DashboardController controller = new DashboardController(spuMapper, orderMapper, memberMapper, refundMapper, mock(DashboardRefreshHub.class));

        Map<String, Object> stats = controller.stats("today", staff).getData();

        assertThat(stats).containsEntry("todayOrders", 5L).containsEntry("pendingRefund", 2L)
                .containsEntry("paidAmount", new BigDecimal("199.90"));
        assertThat((List<Long>) stats.get("hourlyOrderCounts")).hasSize(24).contains(3L);
    }

    @Test
    void twentyFourHourStatsExposeSelectedRangeAndTwentyFourBuckets() {
        SpuMapper spuMapper = mock(SpuMapper.class);
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        MemberMapper memberMapper = mock(MemberMapper.class);
        TradeRefundMapper refundMapper = mock(TradeRefundMapper.class);
        when(orderMapper.countByHourSince(any(LocalDateTime.class))).thenReturn(List.of());
        Authentication staff = mock(Authentication.class);
        when(staff.getPrincipal()).thenReturn(mock(LoginUser.class));

        DashboardController controller = new DashboardController(spuMapper, orderMapper, memberMapper, refundMapper, mock(DashboardRefreshHub.class));

        Map<String, Object> stats = controller.stats("24h", staff).getData();

        assertThat(stats).containsEntry("range", "24h");
        assertThat((List<String>) stats.get("hourLabels")).hasSize(24);
        assertThat((List<Long>) stats.get("hourlyOrderCounts")).hasSize(24);
    }

    @Test
    void memberCannotReadBackofficeStats() {
        Authentication member = mock(Authentication.class);
        when(member.getPrincipal()).thenReturn(mock(MemberPrincipal.class));
        DashboardController controller = new DashboardController(mock(SpuMapper.class), mock(TradeOrderMapper.class),
                mock(MemberMapper.class), mock(TradeRefundMapper.class), mock(DashboardRefreshHub.class));

        assertThatThrownBy(() -> controller.stats("today", member))
                .isInstanceOf(com.mall.common.exception.BusinessException.class)
                .hasMessage("仅后台账户可访问");
    }
}
