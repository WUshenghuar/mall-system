package com.mall.web.controller.system;

import com.mall.trade.mq.TradeDomainEvent;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DashboardRefreshHub {
    static final String CHANNEL = "mall:dashboard:refresh";
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectProvider<RedisMessageListenerContainer> listenerContainerProvider;

    public DashboardRefreshHub(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                               ObjectProvider<RedisMessageListenerContainer> listenerContainerProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.listenerContainerProvider = listenerContainerProvider;
    }

    public SseEmitter subscribe() {
        startRedisSubscription();
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        Runnable remove = () -> {
            emitters.remove(emitter);
            if (emitters.isEmpty()) stopRedisSubscription();
        };
        emitter.onCompletion(remove);
        emitter.onTimeout(() -> {
            remove.run();
            emitter.complete();
        });
        emitter.onError(error -> {
            remove.run();
            emitter.complete();
        });
        try {
            emitter.send(SseEmitter.event().name("ready").data("connected"));
        } catch (IOException | IllegalStateException e) {
            remove.run();
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refresh(TradeDomainEvent event) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            try {
                redis.convertAndSend(CHANNEL, event.type());
                return;
            } catch (RuntimeException ignored) {
                // Redis 不可用时继续刷新当前实例，前端轮询仍可兜底。
            }
        }
        broadcast(event.type());
    }

    void broadcast(String type) {
        // ponytail: in-memory fan-out fits one local instance; use Redis pub/sub for multi-instance deployment.
        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("refresh").data(type));
                return false;
            } catch (IOException | IllegalStateException e) {
                emitter.complete();
                return true;
            }
        });
    }

    private void startRedisSubscription() {
        RedisMessageListenerContainer container = listenerContainerProvider.getIfAvailable();
        if (container != null && !container.isRunning()) {
            try {
                container.start();
            } catch (RuntimeException ignored) {
                // 本地 SSE 和轮询不依赖 Redis 订阅成功。
            }
        }
    }

    private void stopRedisSubscription() {
        RedisMessageListenerContainer container = listenerContainerProvider.getIfAvailable();
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }
}
