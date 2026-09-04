package com.mall.trade.job;

import com.mall.trade.service.TradeOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeOrderTimeoutJob {
    private final TradeOrderService tradeOrderService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelUnpaidOrders() {
        int count = tradeOrderService.cancelExpiredOrders();
        if (count > 0) log.info("已取消 {} 个超时未支付订单", count);
    }
}
