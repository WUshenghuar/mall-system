package com.mall.web.controller.store;

import com.mall.common.result.Result;
import com.mall.marketing.entity.Activity;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.entity.MemberCouponVO;
import com.mall.marketing.service.ActivityService;
import com.mall.marketing.service.CouponService;
import com.mall.security.user.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 提供 C 端可领取优惠券、会员券包和进行中活动查询。 */
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreMarketingController {
    /** 查询可领取优惠券、会员券包及优惠券领取状态。 */
    private final CouponService couponService;
    /** 查询当前有效的营销活动。 */
    private final ActivityService activityService;

    /** 查询当前可领取的优惠券列表。 */
    @GetMapping("/coupons")
    public Result<List<Coupon>> coupons() {
        return Result.success(couponService.listAvailable());
    }

    /** 将指定优惠券发放给当前登录会员。 */
    @PostMapping("/coupons/{couponId}/claim")
    public Result<Void> claim(@PathVariable Long couponId, Authentication auth) {
        couponService.claim(couponId, CurrentMember.id(auth));
        return Result.success(null);
    }

    /** 查询当前会员的优惠券列表及其状态信息。 */
    @GetMapping("/member/coupons")
    public Result<List<MemberCouponVO>> memberCoupons(Authentication auth) {
        return Result.success(couponService.listMemberCoupons(CurrentMember.id(auth)));
    }

    /** 查询当前有效的营销活动列表。 */
    @GetMapping("/activities")
    public Result<List<Activity>> activities() {
        return Result.success(activityService.selectActive());
    }
}
