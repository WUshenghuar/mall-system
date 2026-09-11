package com.mall.trade.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisStockReservationServiceTest {
    @Test
    void releaseRestoresStockAndRefreshesCacheTtl() {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(values);

        new RedisStockReservationService(provider).release(7L, 2);

        verify(values).increment("trade:available-stock:7", 2);
        verify(redis).expire("trade:available-stock:7", Duration.ofHours(6));
    }
}
