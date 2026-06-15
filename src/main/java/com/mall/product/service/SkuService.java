package com.mall.product.service;

import com.mall.product.entity.Sku;

import java.math.BigDecimal;
import java.util.List;

public interface SkuService {
    List<Sku> listBySpuId(Long spuId);
    Sku getById(Long id);
    void save(Sku sku);
    void update(Sku sku);
    void delete(Long id);
    void batchUpdatePrice(List<Long> skuIds, BigDecimal price);
    /** 从 Redis 读取库存 */
    Integer getStock(Long skuId);
    /** 更新 Redis 库存缓存 */
    void updateStock(Long skuId, Integer stock);
}
