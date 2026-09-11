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

@RestController
@RequestMapping("/api/trade/refund")
@RequiredArgsConstructor
public class TradeRefundController {
    private final TradeRefundService tradeRefundService;

    @PostMapping
    public Result<TradeRefund> apply(@Valid @RequestBody ApplyReq req, Authentication auth) {
        return Result.success(tradeRefundService.apply(req.getOrderNo(), CurrentMember.id(auth), req.getReason(),
                req.getRefundType(), req.getEvidenceUrls()));
    }

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "10") Integer size,
                          Authentication auth) {
        return Result.success(tradeRefundService.selectMemberPage(CurrentMember.id(auth), page, size));
    }

    @GetMapping("/{id}")
    public Result<TradeRefund> detail(@PathVariable Long id, Authentication auth) {
        return Result.success(tradeRefundService.getOwnedById(id, CurrentMember.id(auth)));
    }

    @PostMapping("/{id}/return")
    public Result<Void> submitReturn(@PathVariable Long id, @Valid @RequestBody ReturnReq req, Authentication auth) {
        tradeRefundService.submitReturn(id, CurrentMember.id(auth), req.getCompany(), req.getTrackingNo());
        return Result.success(null);
    }

    @Data public static class ApplyReq {
        @NotBlank private String orderNo;
        @NotBlank @Size(max = 500) private String reason;
        @Min(0) @Max(1) private Integer refundType = 0;
        @Size(max = 5) private java.util.List<@NotBlank @Size(max = 500) String> evidenceUrls;
    }

    @Data public static class ReturnReq {
        @NotBlank @Size(max = 64) private String company;
        @NotBlank @Size(max = 128) private String trackingNo;
    }
}
