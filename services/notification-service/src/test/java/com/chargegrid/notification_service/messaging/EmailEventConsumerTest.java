package com.chargegrid.notification_service.messaging;

import com.chargegrid.notification_service.delivery.EmailDeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class EmailEventConsumerTest {
    private final EmailDeliveryService delivery = mock(EmailDeliveryService.class);
    private final EmailEventConsumer consumer = new EmailEventConsumer(new ObjectMapper(), delivery);

    @Test
    void consumesAllSupportedEventTypes() {
        for (String type : new String[]{"ReservationConfirmed", "ChargingSessionStarted", "PaymentSucceeded", "PaymentFailed"}) {
            consumer.onMessage("{\"eventId\":\"" + type + "-1\",\"eventType\":\"" + type + "\",\"email\":\"driver@example.com\"}");
        }
        verify(delivery, times(4)).deliver(anyString(), anyString(), eq("driver@example.com"), anyString(), anyString());
    }

    @Test
    void rejectsMalformedEvents() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> consumer.onMessage("{\"eventType\":\"PaymentSucceeded\"}"));
        verifyNoInteractions(delivery);
    }
}
