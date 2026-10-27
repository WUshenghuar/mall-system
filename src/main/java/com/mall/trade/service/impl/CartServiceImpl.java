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

/** 管理会员购物车条目，并在每次读写时校验条目归属。 */
/**
 * 购物车服务实现类
 * 提供购物车相关的业务逻辑实现，包括添加商品、获取购物车列表、更新购物车、删除购物车等功能
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    /**
     * 购物车数据访问层
     * 用于与数据库进行交互，执行购物车相关的数据操作
     */
    private final TradeCartMapper cartMapper;

    /** 同一会员已存在相同 SKU 时累加数量，并将该条目重新勾选。 */
    @Override
    public TradeCart addCart(Long userId, Long skuId, Integer quantity) {
        if (skuId == null) throw new BusinessException("商品不能为空");
        if (quantity == null || quantity <= 0) throw new BusinessException("商品数量必须大于 0");
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

    /**
     * 根据用户ID获取购物车列表
     * @param userId 用户ID
     * @return 返回该用户的购物车列表，按创建时间降序排列
     */
    @Override
    public List<TradeCart> getCartList(Long userId) {
        // 创建Lambda查询包装器，设置查询条件
        LambdaQueryWrapper<TradeCart> qw = Wrappers.lambdaQuery(TradeCart.class)
                // 设置查询条件：用户ID等于传入的userId
                .eq(TradeCart::getUserId, userId)
                // 设置排序条件：按创建时间降序排列
                .orderByDesc(TradeCart::getCreateTime);
        // 执行查询并返回结果列表
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
