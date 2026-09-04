package com.chargegrid.notification_service.messaging;

import com.chargegrid.notification_service.delivery.EmailDeliveryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EmailEventConsumer {
    private static final Set<String> SUPPORTED = Set.of("ReservationConfirmed", "ChargingSessionStarted", "PaymentSucceeded", "PaymentFailed");
    private final ObjectMapper objectMapper;
    private final EmailDeliveryService deliveryService;

    public EmailEventConsumer(ObjectMapper objectMapper, EmailDeliveryService deliveryService) {
        this.objectMapper = objectMapper; this.deliveryService = deliveryService;
    }

    @RabbitListener(queues = "${notification.email.queue}")
    public void onMessage(String body) {
        try {
            JsonNode event = objectMapper.readTree(body);
            String eventId = required(event, "eventId");
            String eventType = required(event, "eventType");
            if (!SUPPORTED.contains(eventType)) return;
            String recipient = event.hasNonNull("recipient") ? event.get("recipient").asText() : required(event, "email");
            if (!recipient.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new IllegalArgumentException("Invalid recipient email");
            String subject = subjectFor(eventType);
            String text = "ChargeGrid notification: " + eventType;
            deliveryService.deliver(eventId, eventType, recipient, subject, text);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid email event", ex);
        }
    }

    private static String required(JsonNode event, String field) {
        if (!event.hasNonNull(field) || event.get(field).asText().isBlank()) throw new IllegalArgumentException("Missing " + field);
        return event.get(field).asText();
    }

    static String subjectFor(String eventType) {
        return switch (eventType) {
            case "ReservationConfirmed" -> "Your ChargeGrid reservation is confirmed";
            case "ChargingSessionStarted" -> "Your ChargeGrid charging session started";
            case "PaymentSucceeded" -> "Your ChargeGrid payment succeeded";
            case "PaymentFailed" -> "Your ChargeGrid payment failed";
            default -> throw new IllegalArgumentException("Unsupported event type");
        };
    }
}
