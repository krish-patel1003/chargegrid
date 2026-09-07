package com.chargegrid.session_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Session events go to a topic exchange rather than straight to a queue.
 *
 * <p>Two services now care about a completed session — notification-service emails a receipt and
 * billing-service charges the card — and a queue can only be drained by one of them. An exchange
 * lets each bind its own queue and receive its own copy.
 */
@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange eventsExchange(@Value("${chargegrid.events.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }
}
