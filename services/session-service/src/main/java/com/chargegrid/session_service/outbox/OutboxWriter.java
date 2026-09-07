package com.chargegrid.session_service.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Records an event for publication in the caller's transaction. */
@Component
public class OutboxWriter {

    private final OutboxRepository outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxWriter(OutboxRepository outbox, ObjectMapper objectMapper, Clock clock) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Must be called inside the transaction that produced the change, so the event and the state it
     * describes commit together.
     */
    public void write(
            String aggregateType, String aggregateId, String eventType, Map<String, Object> body) {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now(clock);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId.toString());
        envelope.put("eventType", eventType);
        envelope.put("eventVersion", 1);
        envelope.put("occurredAt", now.toString());
        envelope.putAll(body);

        outbox.save(
                new OutboxEvent(
                        eventId,
                        aggregateType,
                        aggregateId,
                        eventType,
                        objectMapper.writeValueAsString(envelope),
                        now));
    }
}
