package com.chargegrid.billing.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A driver's Stripe customer and the card they saved. */
@Entity
@Table(name = "billing_customers")
public class BillingCustomer {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "stripe_customer_id", nullable = false, unique = true)
    private String stripeCustomerId;

    @Column(name = "default_payment_method_id")
    private String defaultPaymentMethodId;

    @Column(name = "card_brand")
    private String cardBrand;

    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BillingCustomer() {}

    public BillingCustomer(String userId, String stripeCustomerId, Instant now) {
        this.userId = userId;
        this.stripeCustomerId = stripeCustomerId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getUserId() {
        return userId;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public String getDefaultPaymentMethodId() {
        return defaultPaymentMethodId;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public boolean hasCard() {
        return defaultPaymentMethodId != null;
    }

    public void saveCard(String paymentMethodId, String brand, String last4, Instant now) {
        this.defaultPaymentMethodId = paymentMethodId;
        this.cardBrand = brand;
        this.cardLast4 = last4;
        this.updatedAt = now;
    }
}
