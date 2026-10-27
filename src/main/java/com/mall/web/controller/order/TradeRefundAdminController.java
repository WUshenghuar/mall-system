package com.mall.web.controller.order;

import com.mall.common.result.Result;
import com.mall.security.user.LoginUser;
import com.mall.trade.service.TradeRefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 提供运营后台退款工单查询、审核和完结入口。 */
@RestController
@RequestMapping("/api/order/trade/refund")
@RequiredArgsConstructor
public class TradeRefundAdminController {
    /** 提供交易退款申请的后台查询、审核和完结能力。 */
    private final TradeRefundService tradeRefundService;

    /** 分页查询后台退款申请；可选状态过滤由退款服务解释处理。 */
    @GetMapping @PreAuthorize("hasAuthority('order:refund:process')")
    public Result<?> page(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Integer status) { return Result.success(tradeRefundService.selectAdminPage(page, size, status)); }

    /** 批准退款申请，并记录当前后台操作人及审核意见。 */
    @PostMapping("/{id}/approve") @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody CommentReq req, Authentication auth) { tradeRefundService.approve(id, ((LoginUser) auth.getPrincipal()).getUserId(), req.getComment()); return Result.success(null); }

    /** 驳回退款申请，并记录当前后台操作人及审核意见。 */
    @PostMapping("/{id}/reject") @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody CommentReq req, Authentication auth) { tradeRefundService.reject(id, ((LoginUser) auth.getPrincipal()).getUserId(), req.getComment()); return Result.success(null); }

    /** 完结退款申请，由服务完成订单状态和退款后库存等处理。 */
    @PostMapping("/{id}/complete") @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result<Void> complete(@PathVariable Long id, @Valid @RequestBody CommentReq req, Authentication auth) { tradeRefundService.complete(id, ((LoginUser) auth.getPrincipal()).getUserId(), req.getComment()); return Result.success(null); }

    /** 后台退款操作说明；审核意见不能为空。 */
    @Data public static class CommentReq { @NotBlank private String comment; }
}
