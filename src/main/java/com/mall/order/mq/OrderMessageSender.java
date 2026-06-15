package com.mall.order.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderMessageSender {
    private final RabbitTemplate rabbitTemplate;

    public void sendOrderCreate(Map<String, Object> message) {
        rabbitTemplate.convertAndSend("order.exchange", "order.create", message);
    }

    public void sendOrderPaid(Map<String, Object> message) {
        rabbitTemplate.convertAndSend("order.exchange", "order.paid", message);
    }
}
