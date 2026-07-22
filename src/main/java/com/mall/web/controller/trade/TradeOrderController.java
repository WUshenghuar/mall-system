package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.LoginUser;
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
        LoginUser user = (LoginUser) auth.getPrincipal();
        return Result.success(tradeOrderService.createOrder(
                user.getUserId(), req.getAddressId(), req.getCouponId(), req.getRemark(), req.getItems()));
    }

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer orderStatus,
            Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        return Result.success(tradeOrderService.selectPage(page, size, user.getUserId(), orderStatus));
    }

    @GetMapping("/{orderNo}")
    public Result<TradeOrder> detail(@PathVariable String orderNo) {
        return Result.success(tradeOrderService.getByOrderNo(orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        tradeOrderService.cancelOrder(orderNo, user.getUserId());
        return Result.success(null);
    }

    @PostMapping("/{orderNo}/confirm")
    public Result<Void> confirm(@PathVariable String orderNo, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        tradeOrderService.confirmReceive(orderNo, user.getUserId());
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
