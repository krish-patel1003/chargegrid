package com.chargegrid.notification_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds this service's own queue to the shared events exchange.
 *
 * <p>Declaring the exchange here too is deliberate: declaration is idempotent, and it means this
 * service starts cleanly whether or not the producer happens to have started first.
 */
@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange eventsExchange(@Value("${chargegrid.events.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    Queue emailQueue(@Value("${notification.email.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    /** Emails are sent for several kinds of event, so every routing key is taken. */
    @Bean
    Binding emailBinding(Queue emailQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(emailQueue).to(eventsExchange).with("#");
    }
}
