package com.mall.web.controller.marketing;

import com.mall.common.result.Result;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 优惠券管理控制器。
 *
 * <p>提供优惠券定义的查询和维护、提交审核及审核处理接口。
 * 通过方法级权限区分优惠券列表查看、定义维护和审核操作；业务规则由优惠券服务负责。</p>
 */
@RestController
@RequestMapping("/api/marketing/coupon")
@RequiredArgsConstructor
public class CouponController {
    // 注入优惠券服务
    private final CouponService couponService;

    /** 分页查询优惠券，可按状态和关键字筛选；页码和页大小默认分别为 1 和 10。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('marketing:coupon:list')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                Integer status, String keyword) {
        return Result.success(couponService.selectPage(page, size, status, keyword));
    }

    /** 按 ID 查询优惠券详情，需要优惠券列表查看权限。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:coupon:list')")
    public Result<Coupon> get(@PathVariable Long id) {
        return Result.success(couponService.getById(id));
    }

    /** 新建优惠券定义，配置内容由请求体提供。 */
    @PostMapping
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result<Void> add(@RequestBody Coupon coupon) {
        couponService.save(coupon);
        return Result.success(null);
    }

    /** 更新指定优惠券；以路径中的 ID 为准设置实体主键后交由服务处理。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Coupon coupon) {
        coupon.setId(id);
        couponService.update(coupon);
        return Result.success(null);
    }

    /** 将指定优惠券提交审核，具体状态校验由优惠券服务执行。 */
    @PostMapping("/{id}/submit-audit")
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result<Void> submitAudit(@PathVariable Long id) {
        couponService.submitAudit(id);
        return Result.success(null);
    }

    /** 审核指定优惠券，通过状态参数和可选审核意见提交审核结果。 */
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('marketing:coupon:audit')")
    public Result<Void> audit(@PathVariable Long id, @RequestParam Integer status,
                               @RequestParam(required = false) String comment) {
        couponService.audit(id, status, comment);
        return Result.success(null);
    }

    /** 删除指定优惠券；是否允许删除由优惠券服务根据业务状态进行判断。 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.success(null);
    }
}
