package com.mall.order.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 将订单事件发往 RabbitMQ 并等待发布确认；确认只保证到达代理，不代表消费完成。 */
/**
 * OrderMessageSender类是一个用于发送订单消息的组件
 * 它依赖于RabbitTemplate，只有在RabbitTemplate存在的情况下才会被创建
 * 使用了Lombok的@RequiredArgsConstructor注解，自动生成final字段的构造器
 */
@Component
@ConditionalOnBean(RabbitTemplate.class) // 只有当RabbitTemplate存在时才创建此Bean
@RequiredArgsConstructor // 自动生成包含final字段的构造器
public class OrderMessageSender {
    // RabbitTemplate用于发送消息到RabbitMQ
    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送订单创建消息
     * @param message 包含订单信息的Map对象
     */
    public void sendOrderCreate(Map<String, Object> message) {
        sendConfirmed("order.create", message);
    }

    /**
     * 发送订单支付完成消息
     * @param message 包含订单信息的Map对象
     */
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
