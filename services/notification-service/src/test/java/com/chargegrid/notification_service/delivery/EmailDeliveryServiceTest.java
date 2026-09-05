package com.chargegrid.notification_service.delivery;

import static org.mockito.Mockito.*;

import com.chargegrid.notification_service.email.EmailProvider;
import org.junit.jupiter.api.Test;

class EmailDeliveryServiceTest {
    private final EmailDeliveryRepository repository = mock(EmailDeliveryRepository.class);
    private final EmailProvider provider = mock(EmailProvider.class);
    private final EmailDeliveryService service =
            new EmailDeliveryService(repository, provider, "no-reply@example.com");

    @Test
    void duplicateEventIsNotSentAgain() {
        when(repository.existsByEventId("event-1")).thenReturn(true);

        service.deliver("event-1", "PaymentSucceeded", "driver@example.com", "Paid", "body");

        verify(provider, never()).send(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void newEventIsPersistedAndSent() {
        when(repository.existsByEventId("event-2")).thenReturn(false);
        when(provider.send(any())).thenReturn("provider-id");

        service.deliver("event-2", "PaymentSucceeded", "driver@example.com", "Paid", "body");

        verify(repository).saveAndFlush(any(EmailDelivery.class));
        verify(provider).send(argThat(email -> email.to().equals("driver@example.com")));
        verify(repository).save(any(EmailDelivery.class));
    }
}
