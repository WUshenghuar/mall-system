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

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreMarketingController {
    private final CouponService couponService;
    private final ActivityService activityService;

    @GetMapping("/coupons")
    public Result<List<Coupon>> coupons() {
        return Result.success(couponService.listAvailable());
    }

    @PostMapping("/coupons/{couponId}/claim")
    public Result<Void> claim(@PathVariable Long couponId, Authentication auth) {
        couponService.claim(couponId, CurrentMember.id(auth));
        return Result.success(null);
    }

    @GetMapping("/member/coupons")
    public Result<List<MemberCouponVO>> memberCoupons(Authentication auth) {
        return Result.success(couponService.listMemberCoupons(CurrentMember.id(auth)));
    }

    @GetMapping("/activities")
    public Result<List<Activity>> activities() {
        return Result.success(activityService.selectActive());
    }
}
