package com.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.marketing.entity.MemberCouponVO;
import com.mall.marketing.entity.ActivitySku;
import com.mall.marketing.service.ActivityService;
import com.mall.marketing.service.CouponService;
import com.mall.finance.service.TaxConfigService;
import com.mall.member.entity.MemberAddress;
import com.mall.member.mapper.MemberAddressMapper;
import com.mall.product.entity.Sku;
import com.mall.product.entity.SkuStock;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SkuStockMapper;
import com.mall.product.mapper.SpuMapper;
import com.mall.trade.entity.TradeCart;
import com.mall.trade.mapper.TradeCartMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettlementServiceImplTest {
    @Test
    void previewAppliesCouponAndReturnsEligibleCoupons() {
        TradeCartMapper cartMapper = mock(TradeCartMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        SkuStockMapper stockMapper = mock(SkuStockMapper.class);
        MemberAddressMapper addressMapper = mock(MemberAddressMapper.class);
        CouponService couponService = mock(CouponService.class);
        MemberAddress address = new MemberAddress(); address.setId(5L); address.setUserId(9L);
        TradeCart cart = new TradeCart(); cart.setId(3L); cart.setSkuId(7L); cart.setQuantity(2); cart.setChecked(1);
        Sku sku = new Sku(); sku.setId(7L); sku.setStatus(1); sku.setSkuCode("SKU-7"); sku.setPrice(new BigDecimal("120"));
        SkuStock stock = new SkuStock(); stock.setSkuId(7L); stock.setStock(10); stock.setLockedStock(0);
        MemberCouponVO available = new MemberCouponVO(); available.setCouponId(7L); available.setStatus(0); available.setThreshold(new BigDecimal("100"));
        when(addressMapper.selectById(5L)).thenReturn(address);
        when(cartMapper.selectList(any())).thenReturn(List.of(cart));
        when(skuMapper.selectById(7L)).thenReturn(sku);
        when(stockMapper.selectOne(any())).thenReturn(stock);
        when(couponService.validateAndCalculateDiscount(9L, 7L, new BigDecimal("240")))
                .thenReturn(new CouponService.DiscountResult(new BigDecimal("20"), 11L));
        when(couponService.listMemberCoupons(9L)).thenReturn(List.of(available));

        Map<String, Object> result = new SettlementServiceImpl(cartMapper, skuMapper, stockMapper, addressMapper, couponService,
                mock(ActivityService.class), mock(SpuMapper.class), mock(TaxConfigService.class))
                .preview(9L, List.of(3L), 5L, 7L);

        assertThat(result).containsEntry("totalAmount", new BigDecimal("240"))
                .containsEntry("discountAmount", new BigDecimal("20"))
                .containsEntry("payAmount", new BigDecimal("220.00"));
        assertThat((List<?>) result.get("availableCoupons")).hasSize(1);
    }

    @Test
    void previewUsesActiveActivityPriceAndStock() {
        TradeCartMapper cartMapper = mock(TradeCartMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        SkuStockMapper stockMapper = mock(SkuStockMapper.class);
        MemberAddressMapper addressMapper = mock(MemberAddressMapper.class);
        CouponService couponService = mock(CouponService.class);
        ActivityService activityService = mock(ActivityService.class);
        MemberAddress address = new MemberAddress(); address.setId(5L); address.setUserId(9L);
        TradeCart cart = new TradeCart(); cart.setId(3L); cart.setSkuId(7L); cart.setQuantity(2); cart.setChecked(1);
        Sku sku = new Sku(); sku.setId(7L); sku.setStatus(1); sku.setSkuCode("SKU-7"); sku.setPrice(new BigDecimal("120"));
        SkuStock stock = new SkuStock(); stock.setSkuId(7L); stock.setStock(10); stock.setLockedStock(0);
        ActivitySku promotion = new ActivitySku(); promotion.setActivityId(8L); promotion.setSkuId(7L);
        promotion.setSeckillPrice(new BigDecimal("80")); promotion.setSeckillStock(5); promotion.setLimitPerUser(1);
        when(addressMapper.selectById(5L)).thenReturn(address);
        when(cartMapper.selectList(any())).thenReturn(List.of(cart));
        when(skuMapper.selectById(7L)).thenReturn(sku);
        when(stockMapper.selectOne(any())).thenReturn(stock);
        when(activityService.findActiveSku(7L)).thenReturn(promotion);
        when(couponService.listMemberCoupons(9L)).thenReturn(List.of());

        Map<String, Object> result = new SettlementServiceImpl(cartMapper, skuMapper, stockMapper, addressMapper,
                        couponService, activityService, mock(SpuMapper.class), mock(TaxConfigService.class))
                .preview(9L, List.of(3L), 5L, null);

        assertThat(result).containsEntry("totalAmount", new BigDecimal("160"))
                .containsEntry("payAmount", new BigDecimal("160.00"));
        Map<?, ?> item = (Map<?, ?>) ((List<?>) result.get("items")).get(0);
        assertThat(item.get("price")).isEqualTo(new BigDecimal("80"));
        assertThat(item.get("activityId")).isEqualTo(8L);
    }

    @Test
    void previewIncludesConfiguredTaxAndCurrency() {
        TradeCartMapper cartMapper = mock(TradeCartMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        SkuStockMapper stockMapper = mock(SkuStockMapper.class);
        MemberAddressMapper addressMapper = mock(MemberAddressMapper.class);
        CouponService couponService = mock(CouponService.class);
        SpuMapper spuMapper = mock(SpuMapper.class);
        TaxConfigService taxService = mock(TaxConfigService.class);
        MemberAddress address = new MemberAddress(); address.setId(5L); address.setUserId(9L); address.setCountry("US");
        TradeCart cart = new TradeCart(); cart.setId(3L); cart.setSkuId(7L); cart.setQuantity(2); cart.setChecked(1);
        Sku sku = new Sku(); sku.setId(7L); sku.setSpuId(20L); sku.setStatus(1); sku.setSkuCode("SKU-7");
        sku.setPrice(new BigDecimal("100")); sku.setCurrency("USD");
        SkuStock stock = new SkuStock(); stock.setSkuId(7L); stock.setStock(10); stock.setLockedStock(0);
        Spu spu = new Spu(); spu.setId(20L); spu.setCategoryId(10L); spu.setOriginCountry("CN");
        when(addressMapper.selectById(5L)).thenReturn(address);
        when(cartMapper.selectList(any())).thenReturn(List.of(cart));
        when(skuMapper.selectById(7L)).thenReturn(sku);
        when(stockMapper.selectOne(any())).thenReturn(stock);
        when(spuMapper.selectById(20L)).thenReturn(spu);
        when(taxService.findApplicableRate(10L, "CN", "US")).thenReturn(new BigDecimal("8"));
        when(couponService.listMemberCoupons(9L)).thenReturn(List.of());

        Map<String, Object> result = new SettlementServiceImpl(cartMapper, skuMapper, stockMapper, addressMapper,
                        couponService, mock(ActivityService.class), spuMapper, taxService).preview(9L, List.of(3L), 5L, null);

        assertThat(result).containsEntry("taxAmount", new BigDecimal("16.00"))
                .containsEntry("currency", "USD").containsEntry("payAmount", new BigDecimal("216.00"));
    }
}
