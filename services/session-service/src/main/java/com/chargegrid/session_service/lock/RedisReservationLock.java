package com.chargegrid.session_service.lock;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty("spring.data.redis.host")
public class RedisReservationLock {
    @Bean
    ReservationLock reservationLock(StringRedisTemplate redis) {
        return (key, timeout) -> {
            String token = UUID.randomUUID().toString();
            long end = System.nanoTime() + timeout.toNanos();
            while (!Boolean.TRUE.equals(
                    redis.opsForValue()
                            .setIfAbsent("chargegrid:reservation:" + key, token, timeout))) {
                if (System.nanoTime() > end) throw new IllegalStateException("connector is busy");
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
            return () -> redis.delete("chargegrid:reservation:" + key);
        };
    }
}
