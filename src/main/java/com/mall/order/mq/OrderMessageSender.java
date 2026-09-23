package com.mall.order.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnBean(RabbitTemplate.class)
@RequiredArgsConstructor
public class OrderMessageSender {
    private final RabbitTemplate rabbitTemplate;

    public void sendOrderCreate(Map<String, Object> message) {
        sendConfirmed("order.create", message);
    }

    public void sendOrderPaid(Map<String, Object> message) {
        sendConfirmed("order.paid", message);
    }

    private void sendConfirmed(String routingKey, Map<String, Object> message) {
        rabbitTemplate.invoke(operations -> {
            operations.convertAndSend("order.exchange", routingKey, message);
            operations.waitForConfirmsOrDie(5_000);
            return null;
        });
    }
}
