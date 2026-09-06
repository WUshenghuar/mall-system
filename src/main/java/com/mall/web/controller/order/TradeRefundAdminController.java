package com.mall.web.controller.order;

import com.mall.common.result.Result;
import com.mall.security.user.LoginUser;
import com.mall.trade.service.TradeRefundService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order/trade/refund")
@RequiredArgsConstructor
public class TradeRefundAdminController {
    private final TradeRefundService tradeRefundService;
    @GetMapping @PreAuthorize("hasAuthority('order:refund:process')")
    public Result<?> page(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Integer status) { return Result.success(tradeRefundService.selectAdminPage(page, size, status)); }
    @PostMapping("/{id}/approve") @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result<Void> approve(@PathVariable Long id, @RequestBody CommentReq req, Authentication auth) { tradeRefundService.approve(id, ((LoginUser) auth.getPrincipal()).getUserId(), req.getComment()); return Result.success(null); }
    @PostMapping("/{id}/reject") @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result<Void> reject(@PathVariable Long id, @RequestBody CommentReq req, Authentication auth) { tradeRefundService.reject(id, ((LoginUser) auth.getPrincipal()).getUserId(), req.getComment()); return Result.success(null); }
    @PostMapping("/{id}/complete") @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result<Void> complete(@PathVariable Long id, @RequestBody CommentReq req, Authentication auth) { tradeRefundService.complete(id, ((LoginUser) auth.getPrincipal()).getUserId(), req.getComment()); return Result.success(null); }
    @Data public static class CommentReq { @NotBlank private String comment; }
}
