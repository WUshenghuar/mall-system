package com.mall.trade.service.impl;

import com.mall.common.exception.BusinessException;
import com.mall.trade.entity.TradeCart;
import com.mall.trade.mapper.TradeCartMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CartServiceImplTest {

    @Test
    void updateCartRejectsAnotherMembersCart() {
        TradeCartMapper mapper = mock(TradeCartMapper.class);
        CartServiceImpl service = new CartServiceImpl(mapper);
        TradeCart cart = new TradeCart();
        cart.setId(10L);
        cart.setUserId(200L);
        cart.setQuantity(1);
        when(mapper.selectById(10L)).thenReturn(cart);

        assertThatThrownBy(() -> service.updateCart(10L, 100L, 2, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权");
    }
}
