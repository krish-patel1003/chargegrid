package com.chargegrid.billing.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Binds settlement to completed sessions only; billing has no interest in other events. */
@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange eventsExchange(@Value("${chargegrid.events.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    Queue settlementQueue(@Value("${chargegrid.events.settlement-queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    Binding settlementBinding(Queue settlementQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(settlementQueue).to(eventsExchange).with("session.completed");
    }
}
