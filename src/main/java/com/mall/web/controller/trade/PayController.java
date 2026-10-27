package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.CurrentMember;
import com.mall.trade.entity.TradePay;
import com.mall.trade.service.PayService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.Map;

/** 提供会员支付单创建、状态查询、受配置控制的模拟支付和网关回调入口。 */
@RestController
@RequestMapping("/api/trade/pay")
@RequiredArgsConstructor
public class PayController {

    /** 创建支付单、查询支付状态并处理支付回调。 */
    private final PayService payService;
    /** 控制是否接收支付宝支付结果通知，默认关闭。 */
    @Value("${trade.payment.callback-enabled:false}")
    private boolean callbackEnabled;

    /** 为当前会员的订单创建支付单。 */
    @PostMapping("/create")
    public Result<TradePay> create(@Valid @RequestBody CreateReq req, Authentication auth) {
        return Result.success(payService.createPay(req.getOrderNo(), req.getPayType(), CurrentMember.id(auth)));
    }

    /** 查询当前会员指定订单的支付状态。 */
    @GetMapping("/status/{orderNo}")
    public Result<TradePay> status(@PathVariable String orderNo, Authentication auth) {
        return Result.success(payService.getPayStatus(orderNo, CurrentMember.id(auth)));
    }

    /** 触发当前会员支付单的模拟成功流程，通常用于开发或测试环境。 */
    @PostMapping("/{payNo}/simulate-success")
    public Result<Void> simulateSuccess(@PathVariable String payNo, Authentication auth) {
        payService.simulateSuccess(payNo, CurrentMember.id(auth));
        return Result.success(null);
    }

    /** 仅在回调开关开启时接收第三方通知，关闭时明确返回失败响应。 */
    @PostMapping("/notify/alipay")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        if (!callbackEnabled) return "failure";
        return payService.handleAlipayNotify(params) ? "success" : "failure";
    }

    /** 创建支付单请求；订单号和支付方式均必填。 */
    @Data
    public static class CreateReq {
        @NotNull private String orderNo;
        @NotNull private Integer payType;
    }
}
