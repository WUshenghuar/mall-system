package com.mall.trade.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.exception.BusinessException;
import com.mall.member.mapper.MemberAddressMapper;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SkuStockMapper;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.mapper.*;
import com.mall.trade.mq.TradeEventPublisher;
import com.mall.trade.service.RedisStockReservationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TradeOrderServiceImplTest {
    @Test
    void orderDetailRejectsAnotherMember() {
        TradeOrderMapper orderMapper = mock(TradeOrderMapper.class);
        TradeOrder order = new TradeOrder();
        order.setOrderNo("T-1");
        order.setUserId(20L);
        when(orderMapper.selectOne(any())).thenReturn(order);
        TradeOrderServiceImpl service = new TradeOrderServiceImpl(orderMapper, mock(TradeOrderItemMapper.class),
                mock(TradeCartMapper.class), mock(TradeLogisticsMapper.class), mock(SkuMapper.class),
                mock(SkuStockMapper.class), mock(MemberAddressMapper.class), new ObjectMapper(),
                mock(TradeEventPublisher.class), mock(RedisStockReservationService.class));

        assertThatThrownBy(() -> service.getOwnedByOrderNo("T-1", 10L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("无权访问");
    }
}
