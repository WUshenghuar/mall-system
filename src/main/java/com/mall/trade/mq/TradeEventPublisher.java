package com.mall.trade.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(String type, String orderNo) {
        applicationEventPublisher.publishEvent(new TradeDomainEvent(type, orderNo));
    }
}
