package com.mall.order.service.impl;

import com.mall.order.entity.OrderRefund;
import com.mall.order.service.RefundService;
import com.mall.web.MallApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = MallApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Sql(scripts = "/test-data-refund.sql")
class RefundServiceImplTest {

    @Autowired
    private RefundService refundService;

    @Test
    @DisplayName("apply: 发起退款申请")
    void shouldApplyRefund() {
        OrderRefund refund = new OrderRefund();
        refund.setOrderId(1L);
        refund.setOrderNo("ORD-REFUND-001");
        refund.setSkuId(100L);
        refund.setQuantity(1);
        refund.setRefundAmount(new BigDecimal("29.99"));
        refund.setRefundReason("商品质量问题");

        refundService.apply(refund, 1L);

        assertThat(refund.getId()).isNotNull();
        assertThat(refund.getRefundStatus()).isEqualTo(0);
        assertThat(refund.getApplicantId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("approve: 审批通过退款")
    void shouldApproveRefund() {
        refundService.approve(400L, 99L, "同意退款");

        OrderRefund refund = refundService.getById(400L);
        assertThat(refund.getRefundStatus()).isEqualTo(1);
        assertThat(refund.getApproverId()).isEqualTo(99L);
        assertThat(refund.getApproveTime()).isNotNull();
    }

    @Test
    @DisplayName("reject: 拒绝退款")
    void shouldRejectRefund() {
        refundService.reject(400L, 99L, "不符合退款条件");

        OrderRefund refund = refundService.getById(400L);
        assertThat(refund.getRefundStatus()).isEqualTo(2);
        assertThat(refund.getApproveComment()).isEqualTo("不符合退款条件");
    }

    @Test
    @DisplayName("approve: 已处理退款抛异常")
    void shouldThrowWhenAlreadyProcessed() {
        assertThatThrownBy(() -> refundService.approve(401L, 99L, null))
                .hasMessageContaining("已处理");
    }

    @Test
    @DisplayName("selectPage: 分页查询退款")
    void shouldPageRefunds() {
        var page = refundService.selectPage(1, 10, null);
        assertThat(page.getRecords()).isNotEmpty();
    }

    @Test
    @DisplayName("selectPage: 按状态筛选")
    void shouldFilterRefundsByStatus() {
        var page = refundService.selectPage(1, 10, 0);
        assertThat(page.getRecords()).allMatch(r -> r.getRefundStatus() == 0);
    }
}
