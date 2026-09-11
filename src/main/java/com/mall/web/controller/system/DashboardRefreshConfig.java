package com.mall.web.controller.system;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(name = "dashboard.redis-pubsub-enabled", havingValue = "true", matchIfMissing = true)
public class DashboardRefreshConfig {
    @Bean
    @Lazy
    public RedisMessageListenerContainer dashboardRefreshListenerContainer(
            RedisConnectionFactory connectionFactory, DashboardRefreshHub refreshHub) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) ->
                refreshHub.broadcast(new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(DashboardRefreshHub.CHANNEL));
        return container;
    }
}
