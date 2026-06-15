package com.mall.web.controller.order;

import com.mall.common.result.Result;
import com.mall.order.entity.Order;
import com.mall.order.service.OrderService;
import com.mall.security.user.LoginUser;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/create")
    public Result<String> create(@RequestBody CreateReq req, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        String orderNo = orderService.createOrder(user.getUserId(),
                req.getSkuId(), req.getQuantity(), req.getCouponId());
        return Result.success(orderNo);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('order:list')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                Integer orderStatus, String keyword) {
        return Result.success(orderService.selectPage(page, size, orderStatus, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:detail')")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        Order order = orderService.getById(id);
        orderService.paySuccess(order.getOrderNo());
        return Result.success(null);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success(null);
    }

    @Data
    public static class CreateReq {
        @NotNull private Long skuId;
        @Min(1) private Integer quantity = 1;
        private Long couponId;
    }
}
