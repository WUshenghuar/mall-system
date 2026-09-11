package com.mall.trade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlementSnapshotServiceTest {
    @Test
    void snapshotCanOnlyBeConsumedOnceByItsOwner() {
        SettlementService settlementService = mock(SettlementService.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        when(settlementService.preview(9L, List.of(3L), 5L, 7L)).thenReturn(Map.of(
                "items", List.of(Map.of("skuId", 7L, "quantity", 2)), "payAmount", 199));
        AtomicReference<String> stored = new AtomicReference<>();
        doAnswer(invocation -> { stored.set(invocation.getArgument(1)); return null; })
                .when(operations).set(startsWith("trade:settlement:snapshot:"), anyString(), eq(Duration.ofMinutes(15)));
        when(operations.getAndDelete(anyString())).thenAnswer(invocation -> stored.getAndSet(null));
        SettlementSnapshotService service = new SettlementSnapshotService(settlementService, redis, new ObjectMapper());

        Map<String, Object> preview = service.create(9L, List.of(3L), 5L, 7L);
        String token = (String) preview.get("snapshotToken");
        SettlementSnapshotService.Snapshot snapshot = service.consume(token, 9L);

        assertThat(snapshot.addressId()).isEqualTo(5L);
        assertThat(snapshot.couponId()).isEqualTo(7L);
        assertThat(snapshot.itemsJson()).contains("\"skuId\":7", "\"quantity\":2");
        assertThatThrownBy(() -> service.consume(token, 9L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已失效");
        verify(operations).set(startsWith("trade:settlement:snapshot:"), anyString(), eq(Duration.ofMinutes(15)));
    }
}
