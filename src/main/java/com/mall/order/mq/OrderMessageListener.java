package com.mall.order.mq;

import com.mall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageListener {
    private final OrderService orderService;

    @RabbitListener(queues = "order.create.queue")
    public void handleOrderCreate(Map<String, Object> message) {
        log.info("Received order create message: {}", message);
        orderService.processOrderMessage(message);
    }

    @RabbitListener(queues = "order.paid.queue")
    public void handleOrderPaid(Map<String, Object> message) {
        String orderNo = (String) message.get("orderNo");
        orderService.paySuccess(orderNo);
    }
}
