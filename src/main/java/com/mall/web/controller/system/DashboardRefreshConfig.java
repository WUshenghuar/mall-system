package com.mall.web.controller.system;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

/** 按配置建立仪表盘 Redis Pub/Sub 监听器，空闲时延迟启动。 */
@Configuration
@ConditionalOnProperty(name = "dashboard.redis-pubsub-enabled", havingValue = "true", matchIfMissing = true)
public class DashboardRefreshConfig {
    /** 创建仪表盘刷新频道的 Redis 监听容器；容器按需启动以避免应用启动时立即连接 Redis。 */
    @Bean
    @Lazy
    public RedisMessageListenerContainer dashboardRefreshListenerContainer(
            RedisConnectionFactory connectionFactory, DashboardRefreshHub refreshHub) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // 收到频道消息后以 UTF-8 解码事件类型，并广播给本实例持有的 SSE 客户端。
        container.addMessageListener((message, pattern) ->
                refreshHub.broadcast(new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(DashboardRefreshHub.CHANNEL));
        return container;
    }
}
