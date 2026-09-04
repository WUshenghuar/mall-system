package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.CurrentMember;
import com.mall.trade.entity.TradePay;
import com.mall.trade.service.PayService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.Map;

@RestController
@RequestMapping("/api/trade/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;
    @Value("${trade.payment.callback-enabled:false}")
    private boolean callbackEnabled;

    @PostMapping("/create")
    public Result<TradePay> create(@RequestBody CreateReq req, Authentication auth) {
        return Result.success(payService.createPay(req.getOrderNo(), req.getPayType(), CurrentMember.id(auth)));
    }

    @GetMapping("/status/{orderNo}")
    public Result<TradePay> status(@PathVariable String orderNo, Authentication auth) {
        return Result.success(payService.getPayStatus(orderNo, CurrentMember.id(auth)));
    }

    @PostMapping("/{payNo}/simulate-success")
    public Result<Void> simulateSuccess(@PathVariable String payNo, Authentication auth) {
        payService.simulateSuccess(payNo, CurrentMember.id(auth));
        return Result.success(null);
    }

    @PostMapping("/notify/alipay")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        if (!callbackEnabled) return "failure";
        payService.handleAlipayNotify(params);
        return "success";
    }

    @PostMapping("/notify/wechat")
    public String wechatNotify(@RequestParam Map<String, String> params) {
        payService.handleWechatNotify(params);
        return "success";
    }

    @Data
    public static class CreateReq {
        @NotNull private String orderNo;
        @NotNull private Integer payType;
    }
}
