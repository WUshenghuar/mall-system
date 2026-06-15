package com.mall.marketing.service.impl;

import com.mall.marketing.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void prepareSeckill(Long activityId, Long skuId, Integer totalStock) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set("seckill:stock:" + skuId, totalStock);
        }
    }

    @Override
    public boolean trySeckill(Long userId, Long skuId) {
        if (redisTemplate == null) return false;
        Long stock = redisTemplate.opsForValue()
                .decrement("seckill:stock:" + skuId);
        if (stock == null || stock < 0) {
            redisTemplate.opsForValue().increment("seckill:stock:" + skuId);
            return false;
        }
        return true;
    }
}
