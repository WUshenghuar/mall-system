package com.mall.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/** 维护 Redis 中的可售库存预留，并与数据库事务提交或回滚保持一致。 */
@Service
@RequiredArgsConstructor
public class RedisStockReservationService {
    private static final DefaultRedisScript<Long> RESERVE = new DefaultRedisScript<>(
            "local n=tonumber(redis.call('GET',KEYS[1]) or '-1'); local q=tonumber(ARGV[1]); "
                    + "if n<q then return 0 end; redis.call('DECRBY',KEYS[1],q); return 1", Long.class);
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    /** 使用 Lua 原子检查并预扣单个 SKU；数据库事务失败时在完成回调中恢复数量。 */
    public boolean reserve(Long skuId, int quantity, int available) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) return true;
        String key = key(skuId);
        redis.opsForValue().setIfAbsent(key, String.valueOf(Math.max(available, 0)), Duration.ofHours(6));
        Long result = redis.execute(RESERVE, List.of(key), String.valueOf(quantity));
        if (!Long.valueOf(1L).equals(result)) return false;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) redis.opsForValue().increment(key, quantity);
                }
            });
        }
        return true;
    }

    /** 在同一段 Lua 脚本中先校验全部 SKU，再统一预扣，避免多商品只扣成功一部分。 */
    public boolean reserveAll(Map<Long, Integer> quantities, Map<Long, Integer> available) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) return true;
        List<String> keys = new ArrayList<>();
        List<String> quantitiesArg = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            String key = key(entry.getKey());
            redis.opsForValue().setIfAbsent(key, String.valueOf(Math.max(available.getOrDefault(entry.getKey(), 0), 0)), Duration.ofHours(6));
            keys.add(key);
            quantitiesArg.add(String.valueOf(entry.getValue()));
        }
        DefaultRedisScript<Long> script = new DefaultRedisScript<>("for i=1,#KEYS do local n=tonumber(redis.call('GET',KEYS[i]) or '-1'); if n<tonumber(ARGV[i]) then return 0 end end; for i=1,#KEYS do redis.call('DECRBY',KEYS[i],ARGV[i]) end; return 1", Long.class);
        if (!Long.valueOf(1L).equals(redis.execute(script, keys, quantitiesArg.toArray()))) return false;
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if (status != STATUS_COMMITTED) quantities.forEach((skuId, qty) -> redis.opsForValue().increment(key(skuId), qty)); }
        });
        return true;
    }

    /** 回补已预留库存；处于事务中时延后到提交后执行，避免回滚后 Redis 与数据库不一致。 */
    public void release(Long skuId, int quantity) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) return;
        String key = key(skuId);
        Runnable action = () -> {
            redis.opsForValue().increment(key, quantity);
            redis.expire(key, Duration.ofHours(6));
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
        } else {
            action.run();
        }
    }

    private String key(Long skuId) { return "trade:available-stock:" + skuId; }
}
