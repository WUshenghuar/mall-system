package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.trade.entity.TradePay;
import com.mall.trade.service.PayService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trade/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    @PostMapping("/create")
    public Result<TradePay> create(@RequestBody CreateReq req) {
        return Result.success(payService.createPay(req.getOrderNo(), req.getPayType()));
    }

    @GetMapping("/status/{orderNo}")
    public Result<TradePay> status(@PathVariable String orderNo) {
        return Result.success(payService.getPayStatus(orderNo));
    }

    @PostMapping("/notify/alipay")
    public String alipayNotify(@RequestParam Map<String, String> params) {
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
