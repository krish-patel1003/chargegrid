package com.chargegrid.billing.settlement;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Turns a completed charging session into a charge on the driver's saved card. */
@Component
public class SessionCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(SessionCompletedConsumer.class);

    private final ObjectMapper objectMapper;
    private final SettlementService settlement;

    public SessionCompletedConsumer(ObjectMapper objectMapper, SettlementService settlement) {
        this.objectMapper = objectMapper;
        this.settlement = settlement;
    }

    @RabbitListener(queues = "${chargegrid.events.settlement-queue}")
    public void onMessage(String body) {
        JsonNode event;
        try {
            event = objectMapper.readTree(body);
        } catch (Exception e) {
            // Unparseable: retrying cannot help.
            throw new IllegalArgumentException("Invalid settlement event", e);
        }
        if (!"ChargingSessionCompleted".equals(text(event, "eventType"))) {
            return;
        }

        String sessionId = text(event, "sessionId");
        String ownerId = text(event, "ownerId");
        if (sessionId == null || ownerId == null) {
            log.warn("Ignoring settlement event without sessionId/ownerId");
            return;
        }

        settlement.settle(
                sessionId,
                ownerId,
                event.hasNonNull("cost") ? event.get("cost").decimalValue() : BigDecimal.ZERO,
                event.hasNonNull("currency") ? event.get("currency").asText() : "usd");
    }

    private static String text(JsonNode event, String field) {
        return event.hasNonNull(field) ? event.get(field).asText() : null;
    }
}
