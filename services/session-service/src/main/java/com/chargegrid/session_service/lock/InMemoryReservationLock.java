package com.chargegrid.session_service.lock;

import java.time.Duration;
import java.util.concurrent.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ReservationLock.class)
public class InMemoryReservationLock implements ReservationLock {
    private final ConcurrentHashMap<String, Semaphore> locks = new ConcurrentHashMap<>();

    public AutoCloseable acquire(String key, Duration timeout) {
        try {
            Semaphore s = locks.computeIfAbsent(key, k -> new Semaphore(1));
            if (!s.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS))
                throw new IllegalStateException("connector is busy");
            return s::release;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("lock interrupted", e);
        }
    }
}
