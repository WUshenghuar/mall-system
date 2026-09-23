package com.mall.order.mq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderMessageSenderTest {
    @Test
    void createMessageWaitsForBrokerConfirmation() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitOperations operations = mock(RabbitOperations.class);
        when(template.invoke(any())).thenAnswer(invocation -> {
            RabbitOperations.OperationsCallback<?> callback = invocation.getArgument(0);
            return callback.doInRabbit(operations);
        });

        new OrderMessageSender(template).sendOrderCreate(Map.of("orderNo", "ORD-1"));

        verify(operations).convertAndSend("order.exchange", "order.create", Map.of("orderNo", "ORD-1"));
        verify(operations).waitForConfirmsOrDie(5_000);
    }
}
