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

/** 提供后台退款申请列表、提交、审批和驳回接口。 */
@RestController
@RequestMapping("/api/order/refund")
@RequiredArgsConstructor
public class RefundController {
    /** 处理传统订单退款申请和后台审批的业务服务。 */
    private final RefundService refundService;

    /** 分页查询后台退款申请，可选按退款状态过滤。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('order:refund:process')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                Integer refundStatus) {
        return Result.success(refundService.selectPage(page, size, refundStatus));
    }

    /** 按退款记录 ID 查询退款申请详情。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:refund:process')")
    public Result<OrderRefund> get(@PathVariable Long id) {
        return Result.success(refundService.getById(id));
    }

    /** 以当前后台用户身份提交退款申请。 */
    @PostMapping
    @PreAuthorize("hasAuthority('order:refund:process')")
    public Result<Void> apply(@Valid @RequestBody OrderRefund refund, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        refundService.apply(refund, user.getUserId());
        return Result.success(null);
    }

    /** 审批退款申请，并将审核人员 ID 和意见交由服务记录。 */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result<Void> approve(@PathVariable Long id, @RequestParam String comment,
                                 Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        refundService.approve(id, user.getUserId(), comment);
        return Result.success(null);
    }

    /** 驳回退款申请，并将审核人员 ID 和意见交由服务记录。 */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result<Void> reject(@PathVariable Long id, @RequestParam String comment,
                                Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        refundService.reject(id, user.getUserId(), comment);
        return Result.success(null);
    }
}
