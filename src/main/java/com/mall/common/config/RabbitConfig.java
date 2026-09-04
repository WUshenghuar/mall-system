package com.mall.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(RabbitTemplate.class)
public class RabbitConfig {

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange");
    }

    @Bean
    public DirectExchange tradeDeadLetterExchange() {
        return new DirectExchange("trade.order.dlx");
    }

    @Bean
    public Queue orderCreateQueue() {
        return new Queue("order.create.queue");
    }

    @Bean
    public Queue orderPaidQueue() {
        return new Queue("order.paid.queue");
    }

    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue())
                .to(orderExchange()).with("order.create");
    }

    @Bean
    public Binding orderPaidBinding() {
        return BindingBuilder.bind(orderPaidQueue())
                .to(orderExchange()).with("order.paid");
    }

    @Bean
    public Queue tradeOrderCreatedQueue() { return tradeQueue("trade.order.created.queue"); }

    @Bean
    public Queue tradeOrderPaidQueue() { return tradeQueue("trade.order.paid.queue"); }

    @Bean
    public Queue tradeOrderCancelledQueue() { return tradeQueue("trade.order.cancelled.queue"); }

    @Bean
    public Queue tradeOrderDeadLetterQueue() {
        return QueueBuilder.durable("trade.order.dead.queue").build();
    }

    @Bean
    public Binding tradeOrderDeadLetterBinding() {
        return BindingBuilder.bind(tradeOrderDeadLetterQueue())
                .to(tradeDeadLetterExchange()).with("trade.order.dead");
    }

    @Bean
    public Binding tradeOrderCreatedBinding() {
        return BindingBuilder.bind(tradeOrderCreatedQueue()).to(orderExchange()).with("trade.order.created");
    }

    @Bean
    public Binding tradeOrderPaidBinding() {
        return BindingBuilder.bind(tradeOrderPaidQueue()).to(orderExchange()).with("trade.order.paid");
    }

    @Bean
    public Binding tradeOrderCancelledBinding() {
        return BindingBuilder.bind(tradeOrderCancelledQueue()).to(orderExchange()).with("trade.order.cancelled");
    }

    private Queue tradeQueue(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange("trade.order.dlx")
                .deadLetterRoutingKey("trade.order.dead")
                .build();
    }
}
