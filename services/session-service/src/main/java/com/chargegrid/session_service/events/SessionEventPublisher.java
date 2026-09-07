package com.chargegrid.session_service.events;

import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

/**
 * Forwards settled sessions to the broker.
 *
 * <p>Two deliberate choices. It listens after commit, so a broker outage can never roll back a
 * charging session the driver already finished. And a publish failure is logged rather than
 * rethrown, because at that point the money-relevant state is already durable.
 *
 * <p>The gap that remains is delivery: a crash between commit and publish loses the event. Closing
 * it properly means a transactional outbox — writing the event in the same transaction and relaying
 * it separately — which is the natural next step rather than anything this class can do.
 */
@Component
public class SessionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SessionEventPublisher.class);

    private final AmqpTemplate events;
    private final ObjectMapper objectMapper;
    private final String routingKey;

    public SessionEventPublisher(
            AmqpTemplate events,
            ObjectMapper objectMapper,
            @Value("${chargegrid.events.routing-key}") String routingKey) {
        this.events = events;
        this.objectMapper = objectMapper;
        this.routingKey = routingKey;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionCompleted(SessionCompleted event) {
        Map<String, Object> envelope =
                Map.of(
                        "eventId", UUID.randomUUID().toString(),
                        "eventType", "ChargingSessionCompleted",
                        "eventVersion", 1,
                        "occurredAt", event.occurredAt().toString(),
                        "sessionId", event.sessionId().toString(),
                        "ownerId", event.ownerId(),
                        "stationId", event.stationId(),
                        "connectorId", event.connectorId(),
                        "energyKwh", event.energyKwh(),
                        "cost", event.cost());
        try {
            events.convertAndSend(routingKey, objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            log.error("Failed to publish session.completed for {}", event.sessionId(), e);
        }
    }
}
