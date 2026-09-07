package com.chargegrid.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @Column(name = "invoice_id", nullable = false, updatable = false, length = 255)
    private String id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InvoiceStatus status;

    @Column(name = "stripe_invoice_id", length = 255)
    private String stripeInvoiceId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    /** Set when the invoice settles a charging session; unique, so a replay cannot double-bill. */
    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Invoice() {}

    public Invoice(
            String id,
            String userId,
            long amount,
            String currency,
            InvoiceStatus status,
            String stripeInvoiceId) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.stripeInvoiceId = stripeInvoiceId;
        this.idempotencyKey = "chargegrid-invoice-" + id;
    }

    /** Settlement of a completed charging session. */
    public static Invoice forSession(
            String sessionId, String userId, long amount, String currency) {
        Invoice invoice = new Invoice();
        invoice.id = "session-" + sessionId;
        invoice.userId = userId;
        invoice.amount = amount;
        invoice.currency = currency;
        invoice.status = InvoiceStatus.DRAFT;
        invoice.sessionId = sessionId;
        invoice.idempotencyKey = "chargegrid-session-" + sessionId;
        return invoice;
    }

    public void paid(String paymentIntentId) {
        this.status = InvoiceStatus.PAID;
        this.stripePaymentIntentId = paymentIntentId;
        this.failureReason = null;
    }

    public void failed(String paymentIntentId, String reason) {
        this.status = InvoiceStatus.FAILED;
        this.stripePaymentIntentId = paymentIntentId;
        this.failureReason =
                reason == null ? null : reason.substring(0, Math.min(reason.length(), 500));
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    @PrePersist
    void create() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void update() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public String getStripeInvoiceId() {
        return stripeInvoiceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
