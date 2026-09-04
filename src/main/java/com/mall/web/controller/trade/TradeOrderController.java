package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.CurrentMember;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.service.TradeOrderService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade/order")
@RequiredArgsConstructor
public class TradeOrderController {

    private final TradeOrderService tradeOrderService;

    @PostMapping
    public Result<TradeOrder> create(@RequestBody CreateReq req, Authentication auth) {
        return Result.success(tradeOrderService.createOrder(
                CurrentMember.id(auth), req.getAddressId(), req.getCouponId(), req.getRemark(), req.getItems()));
    }

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer orderStatus,
            Authentication auth) {
        return Result.success(tradeOrderService.selectPage(page, size, CurrentMember.id(auth), orderStatus));
    }

    @GetMapping("/{orderNo}")
    public Result<TradeOrder> detail(@PathVariable String orderNo, Authentication auth) {
        return Result.success(tradeOrderService.getOwnedByOrderNo(orderNo, CurrentMember.id(auth)));
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo, Authentication auth) {
        tradeOrderService.cancelOrder(orderNo, CurrentMember.id(auth));
        return Result.success(null);
    }

    @PostMapping("/{orderNo}/confirm")
    public Result<Void> confirm(@PathVariable String orderNo, Authentication auth) {
        tradeOrderService.confirmReceive(orderNo, CurrentMember.id(auth));
        return Result.success(null);
    }

    @Data
    public static class CreateReq {
        @NotNull private Long addressId;
        private Long couponId;
        private String remark;
        @NotNull private String items;
    }
}
