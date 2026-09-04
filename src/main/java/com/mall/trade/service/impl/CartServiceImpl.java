package com.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.exception.BusinessException;
import com.mall.trade.entity.TradeCart;
import com.mall.trade.mapper.TradeCartMapper;
import com.mall.trade.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final TradeCartMapper cartMapper;

    @Override
    public TradeCart addCart(Long userId, Long skuId, Integer quantity) {
        LambdaQueryWrapper<TradeCart> qw = Wrappers.lambdaQuery(TradeCart.class)
                .eq(TradeCart::getUserId, userId)
                .eq(TradeCart::getSkuId, skuId);
        TradeCart cart = cartMapper.selectOne(qw);
        if (cart != null) {
            cart.setQuantity(cart.getQuantity() + quantity);
            cart.setChecked(1);
            cartMapper.updateById(cart);
            return cart;
        }
        TradeCart newCart = new TradeCart();
        newCart.setUserId(userId);
        newCart.setSkuId(skuId);
        newCart.setQuantity(quantity);
        newCart.setChecked(1);
        cartMapper.insert(newCart);
        return newCart;
    }

    @Override
    public List<TradeCart> getCartList(Long userId) {
        LambdaQueryWrapper<TradeCart> qw = Wrappers.lambdaQuery(TradeCart.class)
                .eq(TradeCart::getUserId, userId)
                .orderByDesc(TradeCart::getCreateTime);
        return cartMapper.selectList(qw);
    }

    @Override
    public void updateCart(Long id, Long userId, Integer quantity, Integer checked) {
        TradeCart cart = cartMapper.selectById(id);
        if (cart == null) throw new BusinessException("购物车记录不存在");
        if (!userId.equals(cart.getUserId())) throw new BusinessException("无权操作该购物车记录");
        if (quantity != null) {
            if (quantity <= 0) throw new BusinessException("商品数量必须大于 0");
            cart.setQuantity(quantity);
        }
        if (checked != null) {
            if (checked != 0 && checked != 1) throw new BusinessException("勾选状态不合法");
            cart.setChecked(checked);
        }
        cartMapper.updateById(cart);
    }

    @Override
    public void deleteCart(Long id, Long userId) {
        TradeCart cart = cartMapper.selectById(id);
        if (cart == null) throw new BusinessException("购物车记录不存在");
        if (!userId.equals(cart.getUserId())) throw new BusinessException("无权操作该购物车记录");
        cartMapper.deleteById(id);
    }

    @Override
    public void deleteBatch(Long userId, List<Long> ids) {
        LambdaQueryWrapper<TradeCart> qw = Wrappers.lambdaQuery(TradeCart.class)
                .eq(TradeCart::getUserId, userId)
                .in(TradeCart::getId, ids);
        cartMapper.delete(qw);
    }

    @Override
    public Long getCartCount(Long userId) {
        LambdaQueryWrapper<TradeCart> qw = Wrappers.lambdaQuery(TradeCart.class)
                .eq(TradeCart::getUserId, userId);
        return cartMapper.selectCount(qw);
    }
}
