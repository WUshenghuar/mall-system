package com.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BusinessException;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.entity.CouponIssue;
import com.mall.marketing.entity.MemberCouponVO;
import com.mall.marketing.mapper.CouponMapper;
import com.mall.marketing.mapper.CouponIssueMapper;
import com.mall.marketing.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {
    private final CouponMapper couponMapper;
    private final CouponIssueMapper couponIssueMapper;

    @Override
    public IPage<Coupon> selectPage(Integer page, Integer size, Integer status, String keyword) {
        LambdaQueryWrapper<Coupon> wrapper = Wrappers.<Coupon>lambdaQuery()
                .eq(status != null, Coupon::getStatus, status)
                .like(StringUtils.hasText(keyword), Coupon::getCouponName, keyword)
                .orderByDesc(Coupon::getCreateTime);
        return couponMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Coupon getById(Long id) {
        return couponMapper.selectById(id);
    }

    @Override
    @Transactional
    public void save(Coupon coupon) {
        normalizeCoupon(coupon);
        coupon.setStatus(0);
        coupon.setIssuedCount(0);
        couponMapper.insert(coupon);
    }

    @Override
    @Transactional
    public void update(Coupon coupon) {
        normalizeCoupon(coupon);
        couponMapper.updateById(coupon);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        couponMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void submitAudit(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) throw new BusinessException("优惠券不存在");
        coupon.setStatus(1);
        couponMapper.updateById(coupon);
    }

    @Override
    @Transactional
    public void audit(Long id, Integer status, String comment) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) throw new BusinessException("优惠券不存在");
        if (coupon.getStatus() != 1) throw new BusinessException("该优惠券不是待审核状态");
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
    }

    @Override
    public List<Coupon> listAvailable() {
        LocalDateTime now = LocalDateTime.now();
        return couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getStatus, 2)
                .le(Coupon::getValidStart, now).ge(Coupon::getValidEnd, now)
                .and(wrapper -> wrapper.isNull(Coupon::getMaxIssue).or().le(Coupon::getMaxIssue, 0)
                        .or().ltSql(Coupon::getIssuedCount, "max_issue"))
                .orderByAsc(Coupon::getValidEnd));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(Long couponId, Long memberId) {
        Coupon coupon = couponMapper.selectById(couponId);
        LocalDateTime now = LocalDateTime.now();
        if (coupon == null || coupon.getStatus() != 2 || coupon.getValidStart().isAfter(now) || coupon.getValidEnd().isBefore(now)) {
            throw new BusinessException("优惠券不可领取");
        }
        long claimed = couponIssueMapper.selectCount(Wrappers.<CouponIssue>lambdaQuery()
                .eq(CouponIssue::getCouponId, couponId).eq(CouponIssue::getMemberId, memberId));
        if (coupon.getPerLimit() != null && coupon.getPerLimit() > 0 && claimed >= coupon.getPerLimit()) {
            throw new BusinessException("已达到该优惠券领取上限");
        }
        if (couponMapper.increaseIssuedCountIfAvailable(couponId) != 1) throw new BusinessException("优惠券已领完");
        CouponIssue issue = new CouponIssue();
        issue.setCouponId(couponId); issue.setMemberId(memberId); issue.setIssueTime(now); issue.setStatus(0);
        couponIssueMapper.insert(issue);
    }

    @Override
    public List<MemberCouponVO> listMemberCoupons(Long memberId) {
        List<CouponIssue> issues = couponIssueMapper.selectList(Wrappers.<CouponIssue>lambdaQuery()
                .eq(CouponIssue::getMemberId, memberId).orderByDesc(CouponIssue::getIssueTime));
        if (issues.isEmpty()) return List.of();
        Map<Long, Coupon> coupons = couponMapper.selectBatchIds(issues.stream().map(CouponIssue::getCouponId).toList()).stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));
        return issues.stream().map(issue -> toMemberCoupon(issue, coupons.get(issue.getCouponId()))).filter(java.util.Objects::nonNull).toList();
    }

    private MemberCouponVO toMemberCoupon(CouponIssue issue, Coupon coupon) {
        if (coupon == null) return null;
        MemberCouponVO result = new MemberCouponVO();
        result.setIssueId(issue.getId()); result.setCouponId(coupon.getId()); result.setCouponName(coupon.getCouponName());
        result.setCouponType(coupon.getCouponType()); result.setThreshold(coupon.getThreshold()); result.setDiscount(coupon.getDiscount());
        result.setCurrency(coupon.getCurrency()); result.setValidStart(coupon.getValidStart()); result.setValidEnd(coupon.getValidEnd());
        result.setStatus(coupon.getValidEnd().isBefore(LocalDateTime.now()) && issue.getStatus() == 0 ? 2 : issue.getStatus());
        return result;
    }

    private void normalizeCoupon(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        coupon.setCouponType(normalizeCouponType(coupon.getCouponType()));
        coupon.setScope(normalizeScope(coupon.getScope()));
        if (coupon.getCurrency() == null) coupon.setCurrency("CNY");
        if (coupon.getPerLimit() == null || coupon.getPerLimit() < 1) coupon.setPerLimit(1);
        if (coupon.getValidStart() == null) coupon.setValidStart(now);
        if (coupon.getValidEnd() == null) coupon.setValidEnd(coupon.getValidStart().plusDays(30));
        if (!coupon.getValidEnd().isAfter(coupon.getValidStart())) throw new BusinessException("优惠券结束时间必须晚于开始时间");
    }

    private String normalizeCouponType(String value) {
        return switch (String.valueOf(value)) {
            case "1", "DISCOUNT" -> "DISCOUNT";
            case "2", "SHIPPING" -> "SHIPPING";
            default -> "FULL_REDUCTION";
        };
    }

    private String normalizeScope(String value) {
        return switch (String.valueOf(value)) {
            case "1", "CATEGORY" -> "CATEGORY";
            case "2", "SKU" -> "SKU";
            default -> "ALL";
        };
    }
}
