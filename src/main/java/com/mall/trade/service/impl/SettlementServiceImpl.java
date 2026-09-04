package com.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.exception.BusinessException;
import com.mall.member.entity.MemberAddress;
import com.mall.member.mapper.MemberAddressMapper;
import com.mall.product.entity.Sku;
import com.mall.product.entity.SkuStock;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SkuStockMapper;
import com.mall.trade.entity.TradeCart;
import com.mall.trade.mapper.TradeCartMapper;
import com.mall.trade.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {
    private final TradeCartMapper cartMapper;
    private final SkuMapper skuMapper;
    private final SkuStockMapper stockMapper;
    private final MemberAddressMapper addressMapper;

    @Override
    public Map<String, Object> preview(Long userId, List<Long> cartIds, Long addressId) {
        MemberAddress address = addressMapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId())) throw new BusinessException("收货地址不存在或无权使用");
        List<TradeCart> carts = cartMapper.selectList(Wrappers.<TradeCart>lambdaQuery()
                .eq(TradeCart::getUserId, userId).in(TradeCart::getId, cartIds).eq(TradeCart::getChecked, 1));
        if (carts.size() != cartIds.size()) throw new BusinessException("结算商品不存在或未勾选");
        BigDecimal total = BigDecimal.ZERO;
        List<Map<String, Object>> items = new ArrayList<>();
        for (TradeCart cart : carts) {
            Sku sku = skuMapper.selectById(cart.getSkuId());
            SkuStock stock = stockMapper.selectOne(Wrappers.<SkuStock>lambdaQuery().eq(SkuStock::getSkuId, cart.getSkuId()));
            int available = stock == null ? 0 : stock.getStock() - stock.getLockedStock();
            if (sku == null || !Integer.valueOf(1).equals(sku.getStatus()) || available < cart.getQuantity()) {
                throw new BusinessException("存在已下架或库存不足的商品");
            }
            BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            total = total.add(subtotal);
            items.add(Map.of("cartId", cart.getId(), "skuId", sku.getId(), "skuCode", sku.getSkuCode(),
                    "quantity", cart.getQuantity(), "price", sku.getPrice(), "subtotal", subtotal));
        }
        return Map.of("items", items, "totalAmount", total, "discountAmount", BigDecimal.ZERO,
                "freightAmount", BigDecimal.ZERO, "payAmount", total, "address", address);
    }
}
