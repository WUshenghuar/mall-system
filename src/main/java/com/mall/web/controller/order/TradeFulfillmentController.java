package com.mall.web.controller.order;

import com.mall.common.result.Result;
import com.mall.trade.service.TradeOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 提供运营后台发货操作入口，将订单发货和物流信息交由交易服务统一处理。 */
@RestController
@RequestMapping("/api/order/trade")
@RequiredArgsConstructor
public class TradeFulfillmentController {
    /** 更新交易订单发货状态并创建物流信息。 */
    private final TradeOrderService tradeOrderService;

    /** 提交发货及承运信息；订单状态和物流记录由交易订单服务统一处理。 */
    @PostMapping("/{orderNo}/ship")
    @PreAuthorize("hasAuthority('order:edit')")
    public Result<Void> ship(@PathVariable String orderNo, @Valid @RequestBody ShipReq req) {
        tradeOrderService.ship(orderNo, req.getLogisticsNo(), req.getLogisticsCompany());
        return Result.success(null);
    }

    /** 发货请求体；物流单号及承运公司均不能为空。 */
    @Data
    public static class ShipReq {
        @NotBlank private String logisticsNo;
        @NotBlank private String logisticsCompany;
    }
}
