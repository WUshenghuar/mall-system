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

@Service
@RequiredArgsConstructor
public class RedisStockReservationService {
    private static final DefaultRedisScript<Long> RESERVE = new DefaultRedisScript<>(
            "local n=tonumber(redis.call('GET',KEYS[1]) or '-1'); local q=tonumber(ARGV[1]); "
                    + "if n<q then return 0 end; redis.call('DECRBY',KEYS[1],q); return 1", Long.class);
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

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

    public void release(Long skuId, int quantity) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) redis.opsForValue().increment(key(skuId), quantity);
    }

    private String key(Long skuId) { return "trade:available-stock:" + skuId; }
}
