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

/** 管理仪表盘 SSE 客户端，并在多实例时通过 Redis Pub/Sub 扇出刷新事件。 */
@Component
public class DashboardRefreshHub {
    /** 多实例部署时用于同步仪表盘刷新的 Redis Pub/Sub 频道。 */
    static final String CHANNEL = "mall:dashboard:refresh";
    /** 当前应用实例的 SSE 连接集合，使用并发集合以支持请求线程和事件线程并发访问。 */
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    /** 可选 Redis 发布器；未配置 Redis 时仍可向本地 SSE 客户端广播。 */
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    /** 可选 Redis 监听容器；存在时按需启动以接收其他实例发布的事件。 */
    private final ObjectProvider<RedisMessageListenerContainer> listenerContainerProvider;

    /** 注入可选 Redis 组件，允许仪表盘刷新在未启用 Redis Pub/Sub 时继续本地工作。 */
    public DashboardRefreshHub(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                               ObjectProvider<RedisMessageListenerContainer> listenerContainerProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.listenerContainerProvider = listenerContainerProvider;
    }

    /**
     * 注册新的服务器发送事件客户端。
     *
     * <p>客户端首次连接时先尝试启动 Redis 订阅，再登记 emitter 并发送 ready 事件；
     * 客户端关闭、超时或发送失败时移除连接，最后一个连接退出后停止 Redis 监听容器。</p>
     */
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

    /** 在订单领域事件事务提交后发布刷新通知；Redis 发布失败时退回当前实例内存广播。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refresh(TradeDomainEvent event) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            try {
                redis.convertAndSend(CHANNEL, event.type());
                return;
            } catch (RuntimeException ignored) {
                        // Redis 不可用时仍刷新当前实例；其他实例可由前端定时刷新兜底。
            }
        }
        broadcast(event.type());
    }

    /** 向本实例的全部 SSE 客户端发送刷新事件，并清理已失效的连接。 */
    void broadcast(String type) {
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

    /** 若配置了 Redis 监听容器则尝试启动；启动失败不影响本地 SSE 及前端刷新兜底。 */
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

    /** 当本实例已无 SSE 客户端时停止 Redis 监听容器，避免保持不必要的订阅连接。 */
    private void stopRedisSubscription() {
        RedisMessageListenerContainer container = listenerContainerProvider.getIfAvailable();
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }
}
