package com.mall.trade.service.impl;

import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeRefund;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.mapper.TradeRefundMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
        TradeRefundServiceImpl service = new TradeRefundServiceImpl(refundMapper, orderMapper);

        TradeRefund refund = service.apply("T-1", 9L, "不再需要");

        assertThat(refund.getRefundAmount()).isEqualByComparingTo("88.00");
        assertThat(refund.getRefundStatus()).isZero();
        verify(refundMapper).insert(refund);
    }
}
