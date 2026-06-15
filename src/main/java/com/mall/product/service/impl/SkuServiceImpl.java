package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.product.entity.Sku;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.service.SkuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SkuServiceImpl implements SkuService {
    private final SkuMapper skuMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public SkuServiceImpl(SkuMapper skuMapper) {
        this.skuMapper = skuMapper;
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
        if (redisTemplate == null) return 0;
        Object val = redisTemplate.opsForValue().get(STOCK_KEY + skuId);
        if (val == null) return 0;
        return Integer.parseInt(val.toString());
    }

    @Override
    public void updateStock(Long skuId, Integer stock) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(STOCK_KEY + skuId, stock);
        }
    }
}
