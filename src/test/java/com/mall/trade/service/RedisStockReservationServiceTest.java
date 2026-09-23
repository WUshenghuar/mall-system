package com.mall.trade.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Test
    void releaseWaitsUntilTransactionCommit() {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(values);
        TransactionSynchronizationManager.initSynchronization();
        try {
            new RedisStockReservationService(provider).release(7L, 2);
            verify(values, never()).increment("trade:available-stock:7", 2);

            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());

            verify(values).increment("trade:available-stock:7", 2);
            verify(redis).expire("trade:available-stock:7", Duration.ofHours(6));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
