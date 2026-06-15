package com.mall.web.controller.order;

import com.mall.common.result.Result;
import com.mall.order.entity.OrderRefund;
import com.mall.order.service.RefundService;
import com.mall.security.user.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order/refund")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refundService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('order:refund:process')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                Integer refundStatus) {
        return Result.success(refundService.selectPage(page, size, refundStatus));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:refund:process')")
    public Result<OrderRefund> get(@PathVariable Long id) {
        return Result.success(refundService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('order:refund:process')")
    public Result<Void> apply(@Valid @RequestBody OrderRefund refund, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        refundService.apply(refund, user.getUserId());
        return Result.success(null);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result<Void> approve(@PathVariable Long id, @RequestParam String comment,
                                 Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        refundService.approve(id, user.getUserId(), comment);
        return Result.success(null);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result<Void> reject(@PathVariable Long id, @RequestParam String comment,
                                Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        refundService.reject(id, user.getUserId(), comment);
        return Result.success(null);
    }
}
