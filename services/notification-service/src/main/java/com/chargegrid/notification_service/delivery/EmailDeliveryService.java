package com.chargegrid.notification_service.delivery;

import com.chargegrid.notification_service.email.EmailMessage;
import com.chargegrid.notification_service.email.EmailProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailDeliveryService {
    private final EmailDeliveryRepository repository;
    private final EmailProvider provider;
    private final String from;

    public EmailDeliveryService(
            EmailDeliveryRepository repository,
            EmailProvider provider,
            @Value("${notification.email.from}") String from) {
        this.repository = repository;
        this.provider = provider;
        this.from = from;
    }

    @Transactional
    public boolean deliver(
            String eventId, String eventType, String recipient, String subject, String text) {
        if (repository.existsByEventId(eventId)) return false;
        EmailDelivery delivery = new EmailDelivery(eventId, eventType, recipient, subject);
        try {
            repository.saveAndFlush(delivery);
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
        try {
            delivery.markSent(provider.send(new EmailMessage(from, recipient, subject, text)));
        } catch (RuntimeException failure) {
            delivery.markFailed(failure.getMessage());
        }
        repository.save(delivery);
        return true;
    }
}
