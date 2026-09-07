package com.chargegrid.session_service.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies the clock the session lifecycle reads. Injecting it rather than calling {@code
 * Instant.now()} directly is what lets the tests drive reservation expiry deterministically.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
