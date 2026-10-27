package com.mall.web.controller.order;

import com.mall.common.result.Result;
import com.mall.order.entity.Order;
import com.mall.order.service.OrderService;
import com.mall.security.user.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 提供传统订单的创建、查询、支付状态更新和取消接口。 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    /** 处理传统订单的创建、检索、支付成功登记和取消。 */
    private final OrderService orderService;

    /** 以当前后台认证用户的 ID 创建订单，订单参数经请求体校验。 */
    @PostMapping("/create")
    public Result<String> create(@Valid @RequestBody CreateReq req, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        String orderNo = orderService.createOrder(user.getUserId(),
                req.getSkuId(), req.getQuantity(), req.getCouponId());
        return Result.success(orderNo);
    }

    /** 后台分页查询订单，可按状态和关键字筛选。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('order:list')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                Integer orderStatus, String keyword) {
        return Result.success(orderService.selectPage(page, size, orderStatus, keyword));
    }

    /** 按订单数据库 ID 查询订单详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:detail')")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    /** 将指定订单登记为支付成功；支付状态规则由订单服务校验。 */
    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        Order order = orderService.getById(id);
        orderService.paySuccess(order.getOrderNo());
        return Result.success(null);
    }

    /** 取消指定订单，由订单服务检查当前状态并处理后续资源。 */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success(null);
    }

    /** 创建订单的请求体；商品 ID 必填、数量至少为 1，优惠券可选。 */
    @Data
    public static class CreateReq {
        @NotNull private Long skuId;
        @Min(1) private Integer quantity = 1;
        private Long couponId;
    }
}
