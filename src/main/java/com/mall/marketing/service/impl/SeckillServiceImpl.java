package com.mall.marketing.service.impl;

import com.mall.marketing.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** 使用 Redis 维护秒杀 SKU 库存，并提供初始化和原子尝试扣减入口。 */
/**
 * 秒杀服务实现类
 * 提供秒杀商品库存管理功能，包括初始化库存和原子扣减库存操作
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    /**
     * Redis模板对象
     * 用于与Redis数据库进行交互，实现缓存功能
     * 使用required=false表示该依赖不是必须的，即使没有注入该服务也能正常运行
     */
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /** 为秒杀 SKU 写入初始库存；Redis 未启用时不建立秒杀库存。 */
    @Override
    public void prepareSeckill(Long activityId, Long skuId, Integer totalStock) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set("seckill:stock:" + skuId, totalStock);
        }
    }

    /** 原子扣减秒杀库存；扣至负数时立即回补并返回抢购失败。 */
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
