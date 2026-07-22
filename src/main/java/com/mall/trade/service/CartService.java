package com.mall.trade.service;

import com.mall.trade.entity.TradeCart;

import java.util.List;

public interface CartService {
    TradeCart addCart(Long userId, Long skuId, Integer quantity);
    List<TradeCart> getCartList(Long userId);
    void updateCart(Long id, Integer quantity, Integer checked);
    void deleteCart(Long id);
    void deleteBatch(Long userId, List<Long> ids);
    Long getCartCount(Long userId);
}
