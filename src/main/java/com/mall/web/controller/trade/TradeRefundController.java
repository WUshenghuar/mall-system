package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.CurrentMember;
import com.mall.trade.entity.TradeRefund;
import com.mall.trade.service.TradeRefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade/refund")
@RequiredArgsConstructor
public class TradeRefundController {
    private final TradeRefundService tradeRefundService;

    @PostMapping
    public Result<TradeRefund> apply(@Valid @RequestBody ApplyReq req, Authentication auth) {
        return Result.success(tradeRefundService.apply(req.getOrderNo(), CurrentMember.id(auth), req.getReason()));
    }

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
                          Authentication auth) {
        return Result.success(tradeRefundService.selectMemberPage(CurrentMember.id(auth), page, size));
    }

    @GetMapping("/{id}")
    public Result<TradeRefund> detail(@PathVariable Long id, Authentication auth) {
        return Result.success(tradeRefundService.getOwnedById(id, CurrentMember.id(auth)));
    }

    @Data public static class ApplyReq {
        @NotBlank private String orderNo;
        @NotBlank private String reason;
    }
}
