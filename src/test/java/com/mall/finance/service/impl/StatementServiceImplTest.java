package com.mall.finance.service.impl;

import com.mall.finance.entity.Statement;
import com.mall.finance.entity.StatementItem;
import com.mall.finance.mapper.StatementItemMapper;
import com.mall.finance.mapper.StatementMapper;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.entity.TradeRefund;
import com.mall.trade.mapper.TradeOrderMapper;
import com.mall.trade.mapper.TradeRefundMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatementServiceImplTest {
    @Test
    void generateCurrentMonthCreatesStatementAndRefundItem() {
        StatementMapper statementMapper = mock(StatementMapper.class);
        StatementItemMapper itemMapper = mock(StatementItemMapper.class);
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        TradeRefundMapper refundMapper = mock(TradeRefundMapper.class);
        TradeOrder order = new TradeOrder();
        order.setOrderNo("T1"); order.setOrderStatus(6); order.setTotalAmount(new BigDecimal("100"));
        order.setFreightAmount(BigDecimal.ZERO); order.setPayAmount(new BigDecimal("100")); order.setCreateTime(LocalDateTime.now());
        TradeRefund refund = new TradeRefund(); refund.setOrderNo("T1"); refund.setRefundAmount(new BigDecimal("100"));
        when(statementMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectList(any())).thenReturn(List.of(order));
        when(refundMapper.selectList(any())).thenReturn(List.of(refund));
        doAnswer(invocation -> { invocation.<Statement>getArgument(0).setId(1L); return 1; }).when(statementMapper).insert(any(Statement.class));

        new StatementServiceImpl(statementMapper, itemMapper, orderMapper, refundMapper).generateCurrentMonth();

        verify(itemMapper).insert(any(StatementItem.class));
        verify(statementMapper).insert(any(Statement.class));
    }
}
