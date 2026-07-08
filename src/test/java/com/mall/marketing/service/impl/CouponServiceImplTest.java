package com.mall.marketing.service.impl;

import com.mall.marketing.entity.Coupon;
import com.mall.marketing.service.CouponService;
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
@Sql(scripts = "/test-data-coupon.sql")
class CouponServiceImplTest {

    @Autowired
    private CouponService couponService;

    @Test
    @DisplayName("save: 新增优惠券默认为草稿状态")
    void shouldCreateCouponAsDraft() {
        Coupon coupon = new Coupon();
        coupon.setCouponName("测试优惠券");
        coupon.setDiscount(new BigDecimal("10.00"));
        coupon.setThreshold(new BigDecimal("100.00"));
        coupon.setMaxIssue(100);

        couponService.save(coupon);

        assertThat(coupon.getId()).isNotNull();
        assertThat(coupon.getStatus()).isEqualTo(0); // 草稿
        assertThat(coupon.getIssuedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("submitAudit: 草稿提交审核状态变为 1")
    void shouldSubmitForAudit() {
        couponService.submitAudit(200L);

        Coupon coupon = couponService.getById(200L);
        assertThat(coupon.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("audit: 审核通过状态变为 2")
    void shouldApproveCoupon() {
        couponService.audit(201L, 2, "审核通过");

        Coupon coupon = couponService.getById(201L);
        assertThat(coupon.getStatus()).isEqualTo(2);
    }

    @Test
    @DisplayName("audit: 审核不通过状态变为 3")
    void shouldRejectCoupon() {
        couponService.audit(201L, 3, "不符合要求");

        Coupon coupon = couponService.getById(201L);
        assertThat(coupon.getStatus()).isEqualTo(3);
    }

    @Test
    @DisplayName("audit: 非待审核状态抛异常")
    void shouldThrowWhenNotPendingAudit() {
        assertThatThrownBy(() -> couponService.audit(200L, 2, null))
                .hasMessageContaining("不是待审核状态");
    }

    @Test
    @DisplayName("delete: 逻辑删除优惠券")
    void shouldDeleteCoupon() {
        couponService.delete(200L);

        Coupon coupon = couponService.getById(200L);
        assertThat(coupon).isNull();
    }

    @Test
    @DisplayName("selectPage: 分页查询优惠券")
    void shouldPageCoupons() {
        var page = couponService.selectPage(1, 10, null, null);
        assertThat(page.getRecords()).isNotEmpty();
    }
}
