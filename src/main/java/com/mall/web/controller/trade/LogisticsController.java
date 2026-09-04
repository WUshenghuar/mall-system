package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.trade.entity.TradeLogistics;
import com.mall.trade.service.LogisticsService;
import com.mall.trade.service.TradeOrderService;
import com.mall.security.user.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;
    private final TradeOrderService tradeOrderService;

    @GetMapping("/{orderNo}")
    public Result<TradeLogistics> get(@PathVariable String orderNo, Authentication auth) {
        tradeOrderService.getOwnedByOrderNo(orderNo, CurrentMember.id(auth));
        return Result.success(logisticsService.getByOrderNo(orderNo));
    }
}
