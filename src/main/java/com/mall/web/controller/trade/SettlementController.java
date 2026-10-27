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
    /** 根据购物车、地址和优惠券计算下单预览。 */
    private final SettlementService settlementService;
    /** 创建和消费具有时效性的结算快照。 */
    private final SettlementSnapshotService settlementSnapshotService;

    /** 计算当前会员所选购物车商品的结算预览。 */
    @GetMapping
    public Result<Map<String, Object>> preview(@RequestParam List<Long> cartIds,
                                                @RequestParam Long addressId,
                                                @RequestParam(required = false) Long couponId,
                                                Authentication auth) {
        return Result.success(settlementService.preview(CurrentMember.id(auth), cartIds, addressId, couponId));
    }

    /**
     * 根据当前会员提交的购物车项、地址和优惠券生成结算确认快照。
     *
     * <p>后续创建订单时使用快照令牌引用经过服务端校验的结算数据。</p>
     */
    @PostMapping("/check")
    public Result<Map<String, Object>> check(@Valid @RequestBody SettlementReq req, Authentication auth) {
        return Result.success(settlementSnapshotService.create(CurrentMember.id(auth), req.getCartIds(), req.getAddressId(), req.getCouponId()));
    }

    /** 结算确认请求；购物车 ID 列表不能为空，地址和优惠券按服务规则处理。 */
    @Data public static class SettlementReq {
        @NotEmpty private List<Long> cartIds;
        private Long addressId;
        private Long couponId;
    }
}
