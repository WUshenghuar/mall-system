package com.mall.trade.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** 发布应用内交易事件；事务提交后由监听器通知仪表盘刷新。 */
/**
 * 交易事件发布器组件
 * 用于发布交易领域事件，实现事件驱动架构中的事件发布功能
 */
@Component // 将此类标记为Spring组件，由Spring容器管理
@RequiredArgsConstructor // 使用Lombok生成带有final字段的构造函数，实现依赖注入
public class TradeEventPublisher {
    // 应用事件发布器，用于发布Spring应用事件
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布交易领域事件
     * @param type 事件类型，用于区分不同类型的交易事件
     * @param orderNo 订单号，用于标识具体的交易订单
     */
    public void publish(String type, String orderNo) {
        // 创建并发布一个新的交易领域事件
        applicationEventPublisher.publishEvent(new TradeDomainEvent(type, orderNo));
    }
}
