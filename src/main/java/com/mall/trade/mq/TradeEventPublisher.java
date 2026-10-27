package com.mall.trade.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** 发布应用内交易事件；事务提交后由监听器通知仪表盘刷新。 */
@Component
@RequiredArgsConstructor
public class TradeEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(String type, String orderNo) {
        applicationEventPublisher.publishEvent(new TradeDomainEvent(type, orderNo));
    }
}
