package com.mall.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 仅在 RabbitMQ 可用时注册建单和支付消息的交换机、队列及路由绑定。 */
@Configuration
@ConditionalOnBean(RabbitTemplate.class)
public class RabbitConfig {

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange");
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

}
