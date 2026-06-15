package com.mall.marketing.service;

public interface SeckillService {
    /** 秒杀库存预热到 Redis */
    void prepareSeckill(Long activityId, Long skuId, Integer totalStock);
    /** 原子扣减秒杀库存，成功返回 true */
    boolean trySeckill(Long userId, Long skuId);
}
