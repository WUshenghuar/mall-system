package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.trade.entity.TradeLogistics;
import com.mall.trade.service.LogisticsService;
import com.mall.trade.service.TradeOrderService;
import com.mall.security.user.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 提供订单物流查询，并在返回信息前验证订单属于当前会员。 */
@RestController
@RequestMapping("/api/trade/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    /** 查询指定订单的物流记录。 */
    private final LogisticsService logisticsService;
    /** 验证当前会员对订单的访问权。 */
    private final TradeOrderService tradeOrderService;

    /** 先校验订单归属，再返回该订单的物流信息。 */
    @GetMapping("/{orderNo}")
    public Result<TradeLogistics> get(@PathVariable String orderNo, Authentication auth) {
        tradeOrderService.getOwnedByOrderNo(orderNo, CurrentMember.id(auth));
        return Result.success(logisticsService.getByOrderNo(orderNo));
    }
}
