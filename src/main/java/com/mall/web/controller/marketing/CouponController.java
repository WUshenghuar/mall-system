package com.mall.web.controller.marketing;

import com.mall.common.result.Result;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/marketing/coupon")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('marketing:coupon:list')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                Integer status, String keyword) {
        return Result.success(couponService.selectPage(page, size, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:coupon:list')")
    public Result<Coupon> get(@PathVariable Long id) {
        return Result.success(couponService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result<Void> add(@RequestBody Coupon coupon) {
        couponService.save(coupon);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Coupon coupon) {
        coupon.setId(id);
        couponService.update(coupon);
        return Result.success(null);
    }

    @PostMapping("/{id}/submit-audit")
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result<Void> submitAudit(@PathVariable Long id) {
        couponService.submitAudit(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('marketing:coupon:audit')")
    public Result<Void> audit(@PathVariable Long id, @RequestParam Integer status,
                               @RequestParam(required = false) String comment) {
        couponService.audit(id, status, comment);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.success(null);
    }
}
