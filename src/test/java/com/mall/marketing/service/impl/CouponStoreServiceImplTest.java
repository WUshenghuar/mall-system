package com.mall.marketing.service.impl;

import com.mall.marketing.entity.Coupon;
import com.mall.marketing.entity.CouponIssue;
import com.mall.marketing.mapper.CouponIssueMapper;
import com.mall.marketing.mapper.CouponMapper;
import com.mall.marketing.service.CouponService;
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
        when(couponMapper.selectForUpdate(7L)).thenReturn(coupon);
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
        when(couponMapper.selectForUpdate(7L)).thenReturn(coupon);
        when(issueMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> new CouponServiceImpl(couponMapper, issueMapper).claim(7L, 9L))
                .hasMessage("已达到该优惠券领取上限");

        verify(couponMapper, never()).increaseIssuedCountIfAvailable(7L);
        verify(issueMapper, never()).insert(any(CouponIssue.class));
    }

    @Test
    void calculatesDiscountFromRealAmountAndReturnsIssueForAtomicUse() {
        CouponMapper couponMapper = mock(CouponMapper.class);
        CouponIssueMapper issueMapper = mock(CouponIssueMapper.class);
        Coupon coupon = new Coupon();
        coupon.setId(7L); coupon.setStatus(2); coupon.setCouponType("FULL_REDUCTION");
        coupon.setThreshold(new BigDecimal("100")); coupon.setDiscount(new BigDecimal("20"));
        coupon.setValidStart(LocalDateTime.now().minusDays(1)); coupon.setValidEnd(LocalDateTime.now().plusDays(1));
        CouponIssue issue = new CouponIssue();
        issue.setId(11L); issue.setCouponId(7L); issue.setMemberId(9L); issue.setStatus(0);
        when(couponMapper.selectById(7L)).thenReturn(coupon);
        when(issueMapper.selectOne(any())).thenReturn(issue);

        CouponService.DiscountResult result = new CouponServiceImpl(couponMapper, issueMapper)
                .validateAndCalculateDiscount(9L, 7L, new BigDecimal("199"));

        assertThat(result.amount()).isEqualByComparingTo("20.00");
        assertThat(result.issueId()).isEqualTo(11L);
    }

    @Test
    void rejectsUnavailableExpiredAndBelowThresholdCoupons() {
        CouponMapper couponMapper = mock(CouponMapper.class);
        CouponIssueMapper issueMapper = mock(CouponIssueMapper.class);
        Coupon coupon = new Coupon();
        coupon.setId(7L); coupon.setStatus(2); coupon.setThreshold(new BigDecimal("100"));
        coupon.setDiscount(new BigDecimal("20")); coupon.setValidStart(LocalDateTime.now().minusDays(2));
        coupon.setValidEnd(LocalDateTime.now().minusDays(1));
        when(couponMapper.selectById(7L)).thenReturn(coupon);
        CouponService service = new CouponServiceImpl(couponMapper, issueMapper);

        assertThatThrownBy(() -> service.validateAndCalculateDiscount(9L, 7L, new BigDecimal("199")))
                .hasMessage("优惠券不可用");

        coupon.setValidEnd(LocalDateTime.now().plusDays(1));
        when(issueMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.validateAndCalculateDiscount(9L, 7L, new BigDecimal("199")))
                .hasMessage("优惠券不可用");

        CouponIssue issue = new CouponIssue();
        issue.setId(11L); issue.setStatus(0);
        when(issueMapper.selectOne(any())).thenReturn(issue);
        assertThatThrownBy(() -> service.validateAndCalculateDiscount(9L, 7L, new BigDecimal("99")))
                .hasMessage("未满足优惠券使用门槛");
    }
}
