package com.mall.order.service.impl;

import com.mall.order.entity.Order;
import com.mall.order.service.OrderService;
import com.mall.web.MallApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = MallApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Sql(scripts = "/test-data-order.sql")
class OrderServiceImplTest {

    @Autowired
    private OrderService orderService;

    @Test
    @DisplayName("processOrderMessage: 根据 SKU 真实价格创建订单")
    void shouldCreateOrderWithRealSkuPrice() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderNo", "ORD-TEST-001");
        msg.put("userId", 1L);
        msg.put("skuId", 100L);
        msg.put("quantity", 2);

        orderService.processOrderMessage(msg);

        Order order = orderService.getByOrderNo("ORD-TEST-001");
        assertThat(order).isNotNull();
        // SKU-100 价格 = 29.99 USD
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("59.98"));
        assertThat(order.getCurrency()).isEqualTo("USD");
        assertThat(order.getOrderStatus()).isEqualTo(0);
        assertThat(order.getPayAmount())
                .isGreaterThan(order.getTotalAmount()); // 含运费+关税
    }

    @Test
    @DisplayName("processOrderMessage: SKU 不存在时抛异常")
    void shouldThrowWhenSkuNotFound() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderNo", "ORD-TEST-002");
        msg.put("userId", 1L);
        msg.put("skuId", 999L);
        msg.put("quantity", 1);

        assertThatThrownBy(() -> orderService.processOrderMessage(msg))
                .hasMessageContaining("SKU不存在");
    }

    @Test
    @DisplayName("paySuccess: 待支付订单支付成功")
    void shouldMarkOrderAsPaid() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderNo", "ORD-TEST-PAY");
        msg.put("userId", 1L);
        msg.put("skuId", 100L);
        msg.put("quantity", 1);
        orderService.processOrderMessage(msg);

        orderService.paySuccess("ORD-TEST-PAY");

        Order order = orderService.getByOrderNo("ORD-TEST-PAY");
        assertThat(order.getOrderStatus()).isEqualTo(1);
        assertThat(order.getPayStatus()).isEqualTo(1);
        assertThat(order.getPayTime()).isNotNull();
    }

    @Test
    @DisplayName("cancelOrder: 取消订单状态变为 5")
    void shouldCancelOrder() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderNo", "ORD-TEST-CXL");
        msg.put("userId", 1L);
        msg.put("skuId", 100L);
        msg.put("quantity", 1);
        orderService.processOrderMessage(msg);
        Order created = orderService.getByOrderNo("ORD-TEST-CXL");

        orderService.cancelOrder(created.getId());

        Order cancelled = orderService.getById(created.getId());
        assertThat(cancelled.getOrderStatus()).isEqualTo(5);
    }

    @Test
    @DisplayName("selectPage: 按状态筛选分页查询")
    void shouldFilterByStatus() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderNo", "ORD-TEST-PG1");
        msg.put("userId", 1L);
        msg.put("skuId", 100L);
        msg.put("quantity", 1);
        orderService.processOrderMessage(msg);

        var page = orderService.selectPage(1, 10, 0, null);
        assertThat(page.getRecords()).isNotEmpty();
        assertThat(page.getRecords().get(0).getOrderStatus()).isEqualTo(0);
    }

    @Test
    @DisplayName("selectPage: 空关键词查全部")
    void shouldReturnAllWhenNoFilter() {
        var page = orderService.selectPage(1, 10, null, null);
        assertThat(page).isNotNull();
        assertThat(page.getRecords()).isNotNull();
    }
}
