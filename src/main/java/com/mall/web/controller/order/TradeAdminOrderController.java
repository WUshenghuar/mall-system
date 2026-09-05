package com.mall.web.controller.order;

import com.mall.common.result.Result;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.service.TradeOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order/trade")
@RequiredArgsConstructor
public class TradeAdminOrderController {
    private final TradeOrderService tradeOrderService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('order:list')")
    public Result<?> page(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Integer orderStatus,
                          @RequestParam(required = false) String keyword) {
        return Result.success(tradeOrderService.selectAdminPage(page, size, orderStatus, keyword));
    }

    @GetMapping("/{orderNo}")
    @PreAuthorize("hasAuthority('order:detail')")
    public Result<TradeOrder> detail(@PathVariable String orderNo) {
        TradeOrder order = tradeOrderService.getByOrderNo(orderNo);
        if (order == null) return Result.failed(404, "订单不存在");
        return Result.success(order);
    }
}
