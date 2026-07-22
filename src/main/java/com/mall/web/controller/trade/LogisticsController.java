package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.trade.entity.TradeLogistics;
import com.mall.trade.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;

    @GetMapping("/{orderNo}")
    public Result<TradeLogistics> get(@PathVariable String orderNo) {
        return Result.success(logisticsService.getByOrderNo(orderNo));
    }
}
