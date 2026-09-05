package com.chargegrid.notification_service.delivery;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "email_deliveries",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_email_deliveries_event_id", columnNames = "event_id"))
public class EmailDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EmailDelivery() {}

    public EmailDelivery(String eventId, String eventType, String recipient, String subject) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.recipient = recipient;
        this.subject = subject;
        this.status = DeliveryStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getEventId() {
        return eventId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void markSent(String messageId) {
        status = DeliveryStatus.SENT;
        providerMessageId = messageId;
        sentAt = Instant.now();
        updatedAt = Instant.now();
    }

    public void markFailed(String error) {
        status = DeliveryStatus.FAILED;
        errorMessage = error;
        updatedAt = Instant.now();
    }
}
