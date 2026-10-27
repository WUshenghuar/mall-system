package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.CurrentMember;
import com.mall.trade.service.SettlementService;
import com.mall.trade.service.SettlementSnapshotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 提供 C 端结算预览和创建带校验令牌的结算快照。 */
@RestController
@RequestMapping("/api/trade/settle")
@RequiredArgsConstructor
public class SettlementController {
    private final SettlementService settlementService;
    private final SettlementSnapshotService settlementSnapshotService;

    @GetMapping
    public Result<Map<String, Object>> preview(@RequestParam List<Long> cartIds,
                                                @RequestParam Long addressId,
                                                @RequestParam(required = false) Long couponId,
                                                Authentication auth) {
        return Result.success(settlementService.preview(CurrentMember.id(auth), cartIds, addressId, couponId));
    }

    /** 根据当前会员提交的购物车项、地址和优惠券生成结算确认快照。 */
    @PostMapping("/check")
    public Result<Map<String, Object>> check(@Valid @RequestBody SettlementReq req, Authentication auth) {
        return Result.success(settlementSnapshotService.create(CurrentMember.id(auth), req.getCartIds(), req.getAddressId(), req.getCouponId()));
    }

    @Data public static class SettlementReq {
        @NotEmpty private List<Long> cartIds;
        private Long addressId;
        private Long couponId;
    }
}
