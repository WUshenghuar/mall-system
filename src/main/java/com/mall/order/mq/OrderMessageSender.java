package com.mall.order.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 将订单事件发往 RabbitMQ 并等待发布确认；确认只保证到达代理，不代表消费完成。 */
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
