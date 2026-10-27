package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.CurrentMember;
import com.mall.trade.entity.TradeRefund;
import com.mall.trade.service.TradeRefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 提供会员退款申请、进度查询和退货物流提交接口。 */
@RestController
@RequestMapping("/api/trade/refund")
@RequiredArgsConstructor
public class TradeRefundController {
    /** 处理会员退款申请、退款记录查询及退货物流提交。 */
    private final TradeRefundService tradeRefundService;

    /** 为当前会员的订单提交退款或退货退款申请。 */
    @PostMapping
    public Result<TradeRefund> apply(@Valid @RequestBody ApplyReq req, Authentication auth) {
        return Result.success(tradeRefundService.apply(req.getOrderNo(), CurrentMember.id(auth), req.getReason(),
                req.getRefundType(), req.getEvidenceUrls()));
    }

    /** 分页查询当前会员的退款申请。 */
    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
                          Authentication auth) {
        return Result.success(tradeRefundService.selectMemberPage(CurrentMember.id(auth), page, size));
    }

    /** 查询退款申请详情；服务会校验申请属于当前会员。 */
    @GetMapping("/{id}")
    public Result<TradeRefund> detail(@PathVariable Long id, Authentication auth) {
        return Result.success(tradeRefundService.getOwnedById(id, CurrentMember.id(auth)));
    }

    /** 为指定退货退款申请提交承运公司及物流单号。 */
    @PostMapping("/{id}/return")
    public Result<Void> submitReturn(@PathVariable Long id, @Valid @RequestBody ReturnReq req, Authentication auth) {
        tradeRefundService.submitReturn(id, CurrentMember.id(auth), req.getCompany(), req.getTrackingNo());
        return Result.success(null);
    }

    /** 退款申请请求；包含订单号、原因、退款类型及可选凭证地址。 */
    @Data public static class ApplyReq {
        @NotBlank private String orderNo;
        @NotBlank @Size(max = 500) private String reason;
        @Min(0) @Max(1) private Integer refundType = 0;
        @Size(max = 5) private java.util.List<@NotBlank @Size(max = 500) String> evidenceUrls;
    }

    /** 退货物流请求；承运公司及运单号均需提供。 */
    @Data public static class ReturnReq {
        @NotBlank @Size(max = 64) private String company;
        @NotBlank @Size(max = 128) private String trackingNo;
    }
}
