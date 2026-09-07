package com.chargegrid.session_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the queue session events are routed to.
 *
 * <p>Publishing to the default exchange with a routing key that matches no queue is silently
 * dropped by RabbitMQ, so the producer declares the queue itself rather than depending on
 * notification-service having started first. Declaration is idempotent, so both sides may do it.
 */
@Configuration
public class RabbitConfig {

    @Bean
    Queue notificationEmailQueue(@Value("${chargegrid.events.routing-key}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }
}
