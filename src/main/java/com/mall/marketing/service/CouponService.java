package com.mall.marketing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.marketing.entity.Coupon;

public interface CouponService {
    IPage<Coupon> selectPage(Integer page, Integer size, Integer status, String keyword);
    Coupon getById(Long id);
    void save(Coupon coupon);
    void update(Coupon coupon);
    void delete(Long id);
    /** 提交审核（运营） */
    void submitAudit(Long id);
    /** 审核（店长） */
    void audit(Long id, Integer status, String comment);
}
