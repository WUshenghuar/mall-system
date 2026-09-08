package com.mall.marketing.service.impl;

import com.mall.marketing.entity.Coupon;
import com.mall.marketing.entity.CouponIssue;
import com.mall.marketing.mapper.CouponIssueMapper;
import com.mall.marketing.mapper.CouponMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;

class CouponStoreServiceImplTest {
    @Test
    void savingCouponSuppliesAnActiveDefaultValidityWindow() {
        CouponMapper couponMapper = mock(CouponMapper.class);
        CouponIssueMapper issueMapper = mock(CouponIssueMapper.class);
        Coupon coupon = new Coupon();
        coupon.setCouponName("默认有效期券");

        new CouponServiceImpl(couponMapper, issueMapper).save(coupon);

        assertThat(coupon.getValidStart()).isNotNull();
        assertThat(coupon.getValidEnd()).isAfter(coupon.getValidStart());
    }

    @Test
    void memberCanClaimPublishedCouponWithinItsValidPeriod() {
        CouponMapper couponMapper = mock(CouponMapper.class);
        CouponIssueMapper issueMapper = mock(CouponIssueMapper.class);
        Coupon coupon = new Coupon();
        coupon.setId(7L); coupon.setStatus(2); coupon.setCouponName("欢迎券");
        coupon.setDiscount(new BigDecimal("20")); coupon.setValidStart(LocalDateTime.now().minusDays(1));
        coupon.setValidEnd(LocalDateTime.now().plusDays(1)); coupon.setPerLimit(1);
        when(couponMapper.selectById(7L)).thenReturn(coupon);
        when(issueMapper.selectCount(any())).thenReturn(0L);
        when(couponMapper.increaseIssuedCountIfAvailable(7L)).thenReturn(1);

        new CouponServiceImpl(couponMapper, issueMapper).claim(7L, 9L);

        verify(couponMapper).increaseIssuedCountIfAvailable(7L);
        verify(issueMapper).insert(any(CouponIssue.class));
    }

    @Test
    void memberCannotClaimCouponAgainAfterReachingTheLimit() {
        CouponMapper couponMapper = mock(CouponMapper.class);
        CouponIssueMapper issueMapper = mock(CouponIssueMapper.class);
        Coupon coupon = new Coupon();
        coupon.setId(7L); coupon.setStatus(2); coupon.setValidStart(LocalDateTime.now().minusDays(1));
        coupon.setValidEnd(LocalDateTime.now().plusDays(1)); coupon.setPerLimit(1);
        when(couponMapper.selectById(7L)).thenReturn(coupon);
        when(issueMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> new CouponServiceImpl(couponMapper, issueMapper).claim(7L, 9L))
                .hasMessage("已达到该优惠券领取上限");

        verify(couponMapper, never()).increaseIssuedCountIfAvailable(7L);
        verify(issueMapper, never()).insert(any(CouponIssue.class));
    }
}
