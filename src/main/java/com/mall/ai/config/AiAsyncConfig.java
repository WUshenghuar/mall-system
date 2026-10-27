package com.mall.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** 为 SSE 对话提供独立且有界的工作线程池，避免模型调用占用请求线程。 */
@Configuration
public class AiAsyncConfig {
    @Bean
    public AsyncTaskExecutor aiChatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-chat-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
