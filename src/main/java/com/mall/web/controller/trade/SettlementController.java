package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.CurrentMember;
import com.mall.trade.service.SettlementService;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trade/settle")
@RequiredArgsConstructor
public class SettlementController {
    private final SettlementService settlementService;

    @GetMapping
    public Result<Map<String, Object>> preview(@RequestParam List<Long> cartIds,
                                                @RequestParam Long addressId, Authentication auth) {
        return Result.success(settlementService.preview(CurrentMember.id(auth), cartIds, addressId));
    }

    @PostMapping("/check")
    public Result<Map<String, Object>> check(@RequestBody SettlementReq req, Authentication auth) {
        return Result.success(settlementService.preview(CurrentMember.id(auth), req.getCartIds(), req.getAddressId()));
    }

    @Data public static class SettlementReq { @NotEmpty private List<Long> cartIds; private Long addressId; }
}
