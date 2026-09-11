package com.mall.trade.service.impl;

import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeRefund;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.mapper.TradeRefundMapper;
import com.mall.member.service.MemberService;
import com.mall.trade.service.TradeOrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeRefundServiceImplTest {
    @Test
    void paidMemberOrderCreatesOnePendingRefundAndEntersRefundState() {
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        TradeRefundMapper refundMapper = mock(TradeRefundMapper.class);
        TradeOrder order = new TradeOrder();
        order.setOrderNo("T-1"); order.setUserId(9L); order.setOrderStatus(1); order.setPayAmount(new BigDecimal("88.00"));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(refundMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.transitionOwned("T-1", 9L, 1, 5)).thenReturn(1);
        TradeRefundServiceImpl service = new TradeRefundServiceImpl(refundMapper, orderMapper, mock(MemberService.class), mock(TradeOrderService.class));

        TradeRefund refund = service.apply("T-1", 9L, "不再需要", 0, null);

        assertThat(refund.getRefundAmount()).isEqualByComparingTo("88.00");
        assertThat(refund.getRefundType()).isZero();
        assertThat(refund.getOriginalOrderStatus()).isEqualTo(1);
        assertThat(refund.getRefundStatus()).isZero();
        verify(refundMapper).insert(refund);
    }

    @Test
    void returnRefundKeepsOriginalOrderStatusAndEvidenceWhenRejected() {
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        TradeRefundMapper refundMapper = mock(TradeRefundMapper.class);
        TradeOrder order = new TradeOrder();
        order.setOrderNo("T-2"); order.setUserId(9L); order.setOrderStatus(2); order.setPayAmount(new BigDecimal("88.00"));
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(refundMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.transitionOwned("T-2", 9L, 2, 5)).thenReturn(1);
        TradeRefundServiceImpl service = new TradeRefundServiceImpl(refundMapper, orderMapper, mock(MemberService.class), mock(TradeOrderService.class));

        TradeRefund refund = service.apply("T-2", 9L, "商品损坏", 1, List.of("proof-a", "proof-b"));
        refund.setId(2L);
        when(refundMapper.selectById(2L)).thenReturn(refund);
        when(refundMapper.transitionStatus(2L, 0, 2, 7L, "不符合条件")).thenReturn(1);
        when(orderMapper.transitionOwned("T-2", 9L, 5, 2)).thenReturn(1);

        service.reject(2L, 7L, "不符合条件");

        assertThat(refund.getRefundType()).isEqualTo(1);
        assertThat(refund.getOriginalOrderStatus()).isEqualTo(2);
        assertThat(refund.getEvidenceUrls()).isEqualTo("proof-a,proof-b");
        verify(orderMapper).transitionOwned("T-2", 9L, 5, 2);
    }

    @Test
    void returnRefundMustSubmitTrackingBeforeCompletion() {
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        TradeRefundMapper refundMapper = mock(TradeRefundMapper.class);
        MemberService memberService = mock(MemberService.class);
        TradeOrderService tradeOrderService = mock(TradeOrderService.class);
        TradeRefund refund = new TradeRefund();
        refund.setId(3L); refund.setOrderNo("T-3"); refund.setUserId(9L); refund.setRefundType(1);
        refund.setOriginalOrderStatus(3); refund.setRefundAmount(new BigDecimal("88.00")); refund.setRefundStatus(4);
        when(refundMapper.selectById(3L)).thenReturn(refund);
        when(refundMapper.submitReturn(3L, 9L, "DHL", "DHL-001")).thenReturn(1);
        when(refundMapper.transitionStatus(3L, 4, 3, 7L, "已收货")).thenReturn(1);
        when(orderMapper.transitionOwned("T-3", 9L, 5, 6)).thenReturn(1);
        TradeRefundServiceImpl service = new TradeRefundServiceImpl(refundMapper, orderMapper, memberService, tradeOrderService);

        service.submitReturn(3L, 9L, "DHL", "DHL-001");
        service.complete(3L, 7L, "已收货");

        verify(refundMapper).submitReturn(3L, 9L, "DHL", "DHL-001");
        verify(orderMapper).transitionOwned("T-3", 9L, 5, 6);
        verify(tradeOrderService).restoreStockForRefund("T-3", 9L);
        verify(memberService).recordOrderRefund(9L, new BigDecimal("88.00"), "T-3");
    }
}
