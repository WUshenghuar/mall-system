package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.CurrentMember;
import com.mall.trade.entity.TradeOrder;
import com.mall.trade.service.TradeOrderService;
import com.mall.trade.service.SettlementSnapshotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 提供 C 端交易订单接口；下单内容从会员专属结算快照读取。 */
@RestController
@RequestMapping("/api/trade/order")
@RequiredArgsConstructor
public class TradeOrderController {

    /** 创建并维护 C 端交易订单。 */
    private final TradeOrderService tradeOrderService;
    /** 校验并消费与当前会员关联的一次性结算快照。 */
    private final SettlementSnapshotService settlementSnapshotService;

    /** 消费一次性结算快照后创建订单，不接受客户端直接提交商品数量和价格。 */
    @PostMapping
    public Result<TradeOrder> create(@Valid @RequestBody CreateReq req, Authentication auth) {
        Long userId = CurrentMember.id(auth);
        SettlementSnapshotService.Snapshot snapshot = settlementSnapshotService.consume(req.getSnapshotToken(), userId);
        return Result.success(tradeOrderService.createOrder(userId, snapshot.addressId(), snapshot.couponId(), req.getRemark(), snapshot.itemsJson()));
    }

    /** 分页查询当前会员订单，可选按订单状态过滤。 */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer orderStatus,
            Authentication auth) {
        return Result.success(tradeOrderService.selectPage(page, size, CurrentMember.id(auth), orderStatus));
    }

    /** 查询指定订单详情；订单归属由服务校验。 */
    @GetMapping("/{orderNo}")
    public Result<TradeOrder> detail(@PathVariable String orderNo, Authentication auth) {
        return Result.success(tradeOrderService.getOwnedByOrderNo(orderNo, CurrentMember.id(auth)));
    }

    /** 取消当前会员的指定订单；订单状态及资源释放由服务处理。 */
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo, Authentication auth) {
        tradeOrderService.cancelOrder(orderNo, CurrentMember.id(auth));
        return Result.success(null);
    }

    /** 确认当前会员已收到指定订单，并由服务执行状态流转。 */
    @PostMapping("/{orderNo}/confirm")
    public Result<Void> confirm(@PathVariable String orderNo, Authentication auth) {
        tradeOrderService.confirmReceive(orderNo, CurrentMember.id(auth));
        return Result.success(null);
    }

    /** 创建订单请求；仅包含一次性结算快照令牌和可选备注，不直接接受价格或商品数量。 */
    @Data
    public static class CreateReq {
        private String remark;
        @NotNull private String snapshotToken;
    }
}
