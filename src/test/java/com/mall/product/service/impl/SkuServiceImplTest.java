package com.mall.product.service.impl;

import com.mall.product.entity.Sku;
import com.mall.product.service.SkuService;
import com.mall.web.MallApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = MallApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Sql(scripts = "/test-data-sku.sql")
class SkuServiceImplTest {

    @Autowired
    private SkuService skuService;

    @Test
    @DisplayName("getStock: 从 DB 查询库存（无 Redis）")
    void shouldGetStockFromDatabase() {
        Integer stock = skuService.getStock(300L);
        assertThat(stock).isEqualTo(100);
    }

    @Test
    @DisplayName("updateStock: 更新库存同步到 DB")
    void shouldUpdateStockToDatabase() {
        skuService.updateStock(300L, 50);

        Integer stock = skuService.getStock(300L);
        assertThat(stock).isEqualTo(50);
    }

    @Test
    @DisplayName("getById: 查询 SKU")
    void shouldGetSkuById() {
        Sku sku = skuService.getById(300L);
        assertThat(sku).isNotNull();
        assertThat(sku.getSkuCode()).isEqualTo("SKU-TEST-300");
        assertThat(sku.getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
    }

    @Test
    @DisplayName("listBySpuId: 按 SPU 查 SKU 列表")
    void shouldListSkusBySpuId() {
        var skus = skuService.listBySpuId(50L);
        assertThat(skus).isNotEmpty();
        assertThat(skus.get(0).getSpuId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("batchUpdatePrice: 批量更新价格")
    void shouldBatchUpdatePrice() {
        skuService.batchUpdatePrice(java.util.List.of(300L, 301L), new BigDecimal("39.99"));

        Sku sku1 = skuService.getById(300L);
        Sku sku2 = skuService.getById(301L);
        assertThat(sku1.getPrice()).isEqualByComparingTo(new BigDecimal("39.99"));
        assertThat(sku2.getPrice()).isEqualByComparingTo(new BigDecimal("39.99"));
    }

    @Test
    @DisplayName("delete: 删除 SKU 并清除库存缓存")
    void shouldDeleteSku() {
        skuService.delete(300L);

        Sku sku = skuService.getById(300L);
        assertThat(sku).isNull();
    }

    @Test
    @DisplayName("save: 新增 SKU")
    void shouldSaveSku() {
        Sku sku = new Sku();
        sku.setSpuId(50L);
        sku.setSkuCode("SKU-NEW-001");
        sku.setPrice(new BigDecimal("49.99"));
        sku.setCurrency("USD");
        sku.setStatus(1);

        skuService.save(sku);

        assertThat(sku.getId()).isNotNull();
    }
}
