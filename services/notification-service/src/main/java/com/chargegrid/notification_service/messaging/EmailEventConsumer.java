package com.chargegrid.notification_service.messaging;

import com.chargegrid.notification_service.delivery.EmailDeliveryService;
import com.chargegrid.notification_service.directory.RecipientDirectory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailEventConsumer.class);

    private static final String SESSION_COMPLETED = "ChargingSessionCompleted";
    private static final Set<String> SUPPORTED =
            Set.of(
                    "ReservationConfirmed",
                    "ChargingSessionStarted",
                    SESSION_COMPLETED,
                    "PaymentSucceeded",
                    "PaymentFailed");
    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final ObjectMapper objectMapper;
    private final EmailDeliveryService deliveryService;
    private final RecipientDirectory recipients;

    public EmailEventConsumer(
            ObjectMapper objectMapper,
            EmailDeliveryService deliveryService,
            RecipientDirectory recipients) {
        this.objectMapper = objectMapper;
        this.deliveryService = deliveryService;
        this.recipients = recipients;
    }

    @RabbitListener(queues = "${notification.email.queue}")
    public void onMessage(String body) {
        JsonNode event;
        String eventId;
        String eventType;
        try {
            event = objectMapper.readTree(body);
            eventId = required(event, "eventId");
            eventType = required(event, "eventType");
        } catch (Exception e) {
            // Unparseable: retrying cannot help, so reject rather than loop.
            throw new IllegalArgumentException("Invalid email event", e);
        }

        if (!SUPPORTED.contains(eventType)) {
            return;
        }

        String recipient = resolveRecipient(event);
        if (recipient == null) {
            // The event is well formed but nobody can be emailed. Delivery is
            // idempotent on eventId, so dropping it is safe and a redelivery loop
            // would never resolve.
            log.warn("Skipping {} {}: no deliverable recipient", eventType, eventId);
            return;
        }
        if (!recipient.matches(EMAIL_PATTERN)) {
            log.warn("Skipping {} {}: recipient is not a valid address", eventType, eventId);
            return;
        }

        deliveryService.deliver(
                eventId, eventType, recipient, subjectFor(eventType), bodyFor(eventType, event));
    }

    /**
     * Prefers an address carried on the event, then the owner's profile. Letting a transport
     * failure propagate requeues the message; an unknown user does not, because that will not fix
     * itself.
     */
    private String resolveRecipient(JsonNode event) {
        if (event.hasNonNull("recipient")) {
            return event.get("recipient").asText();
        }
        if (event.hasNonNull("email")) {
            return event.get("email").asText();
        }
        if (event.hasNonNull("ownerId")) {
            return recipients
                    .lookup(event.get("ownerId").asText())
                    .map(RecipientDirectory.Recipient::email)
                    .orElse(null);
        }
        return null;
    }

    static String subjectFor(String eventType) {
        return switch (eventType) {
            case "ReservationConfirmed" -> "Your ChargeGrid reservation is confirmed";
            case "ChargingSessionStarted" -> "Your ChargeGrid charging session started";
            case SESSION_COMPLETED -> "Your ChargeGrid charging session receipt";
            case "PaymentSucceeded" -> "Your ChargeGrid payment succeeded";
            case "PaymentFailed" -> "Your ChargeGrid payment failed";
            default -> throw new IllegalArgumentException("Unsupported event type");
        };
    }

    private static String bodyFor(String eventType, JsonNode event) {
        if (!SESSION_COMPLETED.equals(eventType)) {
            return "ChargeGrid notification: " + eventType;
        }
        BigDecimal energy = decimal(event, "energyKwh");
        BigDecimal cost = decimal(event, "cost");
        return "Your charging session is complete. You drew %s kWh for a total of %s %s."
                .formatted(
                        energy.toPlainString(),
                        cost.toPlainString(),
                        event.hasNonNull("currency")
                                ? event.get("currency").asText().toUpperCase()
                                : "USD");
    }

    private static BigDecimal decimal(JsonNode event, String field) {
        return event.hasNonNull(field) ? event.get(field).decimalValue() : BigDecimal.ZERO;
    }

    private static String required(JsonNode event, String field) {
        if (!event.hasNonNull(field) || event.get(field).asText().isBlank()) {
            throw new IllegalArgumentException("Missing " + field);
        }
        return event.get(field).asText();
    }
}
