package com.mall.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.product.entity.Sku;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StoreCatalogServiceImplTest {
    @Test
    void productsExposeCheapestActiveSkuAsCatalogCardData() {
        SpuMapper spuMapper = mock(SpuMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        Spu spu = new Spu();
        spu.setId(7L);
        spu.setSpuName("Travel Adapter");
        Page<Spu> page = new Page<>(1, 20);
        page.setRecords(List.of(spu));
        when(spuMapper.selectPage(any(), any())).thenReturn(page);

        Sku sku = new Sku();
        sku.setSpuId(7L);
        sku.setPrice(new BigDecimal("19.90"));
        sku.setCurrency("USD");
        sku.setStatus(1);
        sku.setImages("[\"https://cdn.example/adapter.jpg\"]");
        when(skuMapper.selectList(any())).thenReturn(List.of(sku));

        StoreCatalogServiceImpl service = new StoreCatalogServiceImpl(mock(CategoryMapper.class), spuMapper, skuMapper, new ObjectMapper());

        Spu card = (Spu) service.products(1, 20, null, null).getRecords().get(0);

        assertThat(card.getMinPrice()).isEqualByComparingTo("19.90");
        assertThat(card.getCurrency()).isEqualTo("USD");
        assertThat(card.getCoverImage()).isEqualTo("https://cdn.example/adapter.jpg");
    }
}
