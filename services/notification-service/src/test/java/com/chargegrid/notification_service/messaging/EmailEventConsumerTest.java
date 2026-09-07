package com.chargegrid.notification_service.messaging;

import static org.mockito.Mockito.*;

import com.chargegrid.notification_service.delivery.EmailDeliveryService;
import com.chargegrid.notification_service.directory.RecipientDirectory;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class EmailEventConsumerTest {

    private static final String OWNER = "8f1c9a6e-0000-4000-8000-000000000001";

    private final EmailDeliveryService delivery = mock(EmailDeliveryService.class);
    private final RecipientDirectory recipients = mock(RecipientDirectory.class);
    private final EmailEventConsumer consumer =
            new EmailEventConsumer(new ObjectMapper(), delivery, recipients);

    @Test
    void consumesAllSupportedEventTypes() {
        for (String type :
                new String[] {
                    "ReservationConfirmed",
                    "ChargingSessionStarted",
                    "ChargingSessionCompleted",
                    "PaymentSucceeded",
                    "PaymentFailed"
                }) {
            consumer.onMessage(
                    "{\"eventId\":\""
                            + type
                            + "-1\",\"eventType\":\""
                            + type
                            + "\",\"email\":\"driver@example.com\"}");
        }
        verify(delivery, times(5))
                .deliver(
                        anyString(),
                        anyString(),
                        eq("driver@example.com"),
                        anyString(),
                        anyString());
    }

    @Test
    void resolvesTheRecipientFromTheOwnerWhenTheEventCarriesNoAddress() {
        when(recipients.lookup(OWNER))
                .thenReturn(
                        Optional.of(
                                new RecipientDirectory.Recipient(
                                        OWNER, "driver@example.com", "Demo Driver")));

        consumer.onMessage(completedEvent());

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(delivery)
                .deliver(
                        eq("evt-1"),
                        eq("ChargingSessionCompleted"),
                        eq("driver@example.com"),
                        eq("Your ChargeGrid charging session receipt"),
                        body.capture());
        // The receipt states what the driver actually used and paid.
        org.assertj.core.api.Assertions.assertThat(body.getValue())
                .contains("9.25 kWh")
                .contains("3.89 USD");
    }

    @Test
    void skipsDeliveryWhenTheOwnerHasNoProfile() {
        when(recipients.lookup(OWNER)).thenReturn(Optional.empty());

        consumer.onMessage(completedEvent());

        // Nothing to send to, and redelivery would not change that.
        verifyNoInteractions(delivery);
    }

    @Test
    void letsADirectoryOutageRequeueTheMessage() {
        when(recipients.lookup(OWNER))
                .thenThrow(
                        new RecipientDirectory.DirectoryUnavailableException(
                                OWNER, new RuntimeException("connect timed out")));

        Assertions.assertThrows(RuntimeException.class, () -> consumer.onMessage(completedEvent()));
        verifyNoInteractions(delivery);
    }

    @Test
    void ignoresEventTypesItDoesNotHandle() {
        consumer.onMessage(
                "{\"eventId\":\"evt-9\",\"eventType\":\"SomethingElse\",\"email\":\"d@e.com\"}");
        verifyNoInteractions(delivery);
    }

    @Test
    void rejectsMalformedEvents() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> consumer.onMessage("{\"eventType\":\"PaymentSucceeded\"}"));
        verifyNoInteractions(delivery);
    }

    private static String completedEvent() {
        return "{\"eventId\":\"evt-1\",\"eventType\":\"ChargingSessionCompleted\",\"ownerId\":\""
                + OWNER
                + "\",\"energyKwh\":9.25,\"cost\":3.89,\"currency\":\"usd\"}";
    }
}
