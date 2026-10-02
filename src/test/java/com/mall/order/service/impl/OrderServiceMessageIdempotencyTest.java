package com.mall.order.service.impl;

import com.mall.finance.mapper.TaxConfigMapper;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.product.entity.Sku;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.mapper.SpuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceMessageIdempotencyTest {
    @Test
    void concurrentDuplicateMessagesCreateOnlyOneOrder() throws Exception {
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderItemMapper itemMapper = mock(OrderItemMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        CyclicBarrier lookups = new CyclicBarrier(2);
        AtomicBoolean orderExists = new AtomicBoolean();
        AtomicInteger itemInserts = new AtomicInteger();
        when(orderMapper.selectCount(any())).thenAnswer(invocation -> {
            lookups.await(5, TimeUnit.SECONDS);
            return 0L;
        });
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (!orderExists.compareAndSet(false, true)) throw new DuplicateKeyException("duplicate order_no");
            order.setId(1L);
            return 1;
        }).when(orderMapper).insert(any(Order.class));
        doAnswer(invocation -> itemInserts.incrementAndGet()).when(itemMapper).insert(any(OrderItem.class));
        Sku sku = new Sku();
        sku.setPrice(BigDecimal.ONE);
        when(skuMapper.selectById(7L)).thenReturn(sku);
        OrderServiceImpl service = new OrderServiceImpl(orderMapper, itemMapper, skuMapper,
                mock(SpuMapper.class), mock(TaxConfigMapper.class));
        Map<String, Object> message = Map.of("orderNo", "ORD-RACE", "userId", 1L, "skuId", 7L, "quantity", 2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> service.processOrderMessage(message));
            var second = executor.submit(() -> service.processOrderMessage(message));
            assertThatCode(() -> {
                first.get(5, TimeUnit.SECONDS);
                second.get(5, TimeUnit.SECONDS);
            }).doesNotThrowAnyException();
        } finally {
            executor.shutdownNow();
        }

        verify(orderMapper, times(2)).insert(any(Order.class));
        verify(itemMapper, times(1)).insert(any(OrderItem.class));
        org.assertj.core.api.Assertions.assertThat(itemInserts).hasValue(1);
    }
}
