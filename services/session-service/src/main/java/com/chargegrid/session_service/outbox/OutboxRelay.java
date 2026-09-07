package com.chargegrid.session_service.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves outbox rows to the broker.
 *
 * <p>Runs on a timer rather than reacting to the write, so an event still leaves the outbox after a
 * broker outage or a crash — the row is already committed, and the next tick picks it up. A row
 * that keeps failing stays unpublished and its {@code attempts} and {@code last_error} say why.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxRepository outbox;
    private final AmqpTemplate broker;
    private final String exchange;
    private final Clock clock;

    public OutboxRelay(
            OutboxRepository outbox,
            AmqpTemplate broker,
            @Value("${chargegrid.events.exchange}") String exchange,
            Clock clock) {
        this.outbox = outbox;
        this.broker = broker;
        this.exchange = exchange;
        this.clock = clock;
    }

    /** e.g. ChargingSessionCompleted becomes session.completed. */
    private static String routingKeyFor(OutboxEvent event) {
        return switch (event.getEventType()) {
            case "ChargingSessionCompleted" -> "session.completed";
            default -> "session." + event.getEventType().toLowerCase();
        };
    }

    @Scheduled(fixedDelayString = "${chargegrid.events.relay-interval-ms:1000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = outbox.claimUnpublished(Limit.of(BATCH_SIZE));
        for (OutboxEvent event : batch) {
            try {
                broker.convertAndSend(exchange, routingKeyFor(event), event.getPayload());
                event.markPublished(Instant.now(clock));
            } catch (Exception e) {
                // Leave it unpublished; the next tick retries. Publishing is
                // at-least-once, so a duplicate is safer than a dropped event.
                event.markFailed(e.getMessage());
                log.warn(
                        "Outbox publish failed for {} (attempt {}): {}",
                        event.getId(),
                        event.getAttempts(),
                        e.getMessage());
            }
        }
    }
}
