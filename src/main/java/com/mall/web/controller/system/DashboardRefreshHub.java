package com.mall.web.controller.system;

import com.mall.trade.mq.TradeDomainEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DashboardRefreshHub {
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        Runnable remove = () -> emitters.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
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
        // ponytail: in-memory fan-out fits one local instance; use Redis pub/sub for multi-instance deployment.
        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("refresh").data(event.type()));
                return false;
            } catch (IOException | IllegalStateException e) {
                emitter.complete();
                return true;
            }
        });
    }
}
