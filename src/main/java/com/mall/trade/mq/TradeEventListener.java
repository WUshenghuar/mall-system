package com.mall.trade.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TradeEventListener {
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(TradeDomainEvent event) {
        RabbitTemplate template = rabbitTemplateProvider.getIfAvailable();
        if (template != null) template.convertAndSend("order.exchange", "trade.order." + event.type(), event);
    }
}
