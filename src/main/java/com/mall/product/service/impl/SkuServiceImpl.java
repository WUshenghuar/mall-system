package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.product.entity.Sku;
import com.mall.product.entity.SkuStock;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SkuStockMapper;
import com.mall.product.service.SkuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SkuServiceImpl implements SkuService {
    private final SkuMapper skuMapper;
    private final SkuStockMapper skuStockMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public SkuServiceImpl(SkuMapper skuMapper,
                           SkuStockMapper skuStockMapper) {
        this.skuMapper = skuMapper;
        this.skuStockMapper = skuStockMapper;
    }

    private static final String STOCK_KEY = "stock:sku:";

    @Override
    public List<Sku> listBySpuId(Long spuId) {
        return skuMapper.selectList(
                Wrappers.<Sku>lambdaQuery().eq(Sku::getSpuId, spuId));
    }

    @Override
    public Sku getById(Long id) {
        return skuMapper.selectById(id);
    }

    @Override
    public void save(Sku sku) {
        skuMapper.insert(sku);
    }

    @Override
    public void update(Sku sku) {
        skuMapper.updateById(sku);
    }

    @Override
    public void delete(Long id) {
        skuMapper.deleteById(id);
        if (redisTemplate != null) {
            redisTemplate.delete(STOCK_KEY + id);
        }
    }

    @Override
    public void batchUpdatePrice(List<Long> skuIds, BigDecimal price) {
        for (Long id : skuIds) {
            Sku sku = skuMapper.selectById(id);
            if (sku != null) {
                sku.setPrice(price);
                skuMapper.updateById(sku);
            }
        }
    }

    @Override
    public Integer getStock(Long skuId) {
        // Redis 可用时优先从缓存读取
        if (redisTemplate != null) {
            Object val = redisTemplate.opsForValue().get(STOCK_KEY + skuId);
            if (val != null) return Integer.parseInt(val.toString());
        }
        // 回退到数据库
        SkuStock skuStock = skuStockMapper.selectOne(
                Wrappers.<SkuStock>lambdaQuery().eq(SkuStock::getSkuId, skuId));
        return skuStock != null ? skuStock.getStock() : 0;
    }

    @Override
    public void updateStock(Long skuId, Integer stock) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(STOCK_KEY + skuId, stock);
        }
        // 同步更新数据库
        SkuStock skuStock = skuStockMapper.selectOne(
                Wrappers.<SkuStock>lambdaQuery().eq(SkuStock::getSkuId, skuId));
        if (skuStock != null) {
            skuStock.setStock(stock);
            skuStockMapper.updateById(skuStock);
        }
    }
}
