package com.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.mapper.CouponMapper;
import com.mall.marketing.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {
    private final CouponMapper couponMapper;

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
        coupon.setStatus(0);
        coupon.setIssuedCount(0);
        couponMapper.insert(coupon);
    }

    @Override
    @Transactional
    public void update(Coupon coupon) {
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
        if (coupon == null) throw new RuntimeException("优惠券不存在");
        coupon.setStatus(1);
        couponMapper.updateById(coupon);
    }

    @Override
    @Transactional
    public void audit(Long id, Integer status, String comment) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) throw new RuntimeException("优惠券不存在");
        if (coupon.getStatus() != 1) throw new RuntimeException("该优惠券不是待审核状态");
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
    }
}
