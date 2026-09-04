package com.mall.web.controller.order;

import com.mall.common.result.Result;
import com.mall.trade.service.TradeOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order/trade")
@RequiredArgsConstructor
public class TradeFulfillmentController {
    private final TradeOrderService tradeOrderService;

    @PostMapping("/{orderNo}/ship")
    @PreAuthorize("hasAuthority('order:edit')")
    public Result<Void> ship(@PathVariable String orderNo, @Valid @RequestBody ShipReq req) {
        tradeOrderService.ship(orderNo, req.getLogisticsNo(), req.getLogisticsCompany());
        return Result.success(null);
    }

    @Data
    public static class ShipReq {
        @NotBlank private String logisticsNo;
        @NotBlank private String logisticsCompany;
    }
}
