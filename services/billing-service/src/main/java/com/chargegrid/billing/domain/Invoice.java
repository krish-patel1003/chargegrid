package com.chargegrid.billing.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "invoices")
public class Invoice {
    @Id @Column(name = "invoice_id", nullable = false, updatable = false, length = 255) private String id;
    @Column(name = "user_id", nullable = false, length = 255) private String userId;
    @Column(nullable = false) private long amount;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private InvoiceStatus status;
    @Column(name = "stripe_invoice_id", length = 255) private String stripeInvoiceId;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255) private String idempotencyKey;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Invoice() {}
    public Invoice(String id, String userId, long amount, String currency, InvoiceStatus status, String stripeInvoiceId) {
        this.id = id; this.userId = userId; this.amount = amount; this.currency = currency; this.status = status;
        this.stripeInvoiceId = stripeInvoiceId; this.idempotencyKey = "chargegrid-invoice-" + id;
    }
    @PrePersist void create() { var now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void update() { updatedAt = Instant.now(); }
    public String getId() { return id; } public String getUserId() { return userId; } public long getAmount() { return amount; }
    public String getCurrency() { return currency; } public InvoiceStatus getStatus() { return status; }
    public String getStripeInvoiceId() { return stripeInvoiceId; } public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
