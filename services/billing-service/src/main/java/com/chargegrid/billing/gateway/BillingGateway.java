package com.chargegrid.billing.gateway;

public interface BillingGateway {
    CustomerResult createCustomer(String userId, String email, String name);

    SetupIntentResult createSetupIntent(String customerId);

    PaymentMethodResult getPaymentMethod(String paymentMethodId);

    /** The payment method a completed SetupIntent saved, so the card can be stored as default. */
    String paymentMethodFromSetupIntent(String setupIntentId);

    void setDefaultPaymentMethod(String customerId, String paymentMethodId);

    /**
     * Charges a saved card with the driver not present.
     *
     * <p>{@code idempotencyKey} is what makes redelivery of a completed-session event safe: Stripe
     * returns the original charge rather than creating a second one.
     */
    PaymentResult chargeOffSession(
            String customerId,
            String paymentMethodId,
            long amountMinorUnits,
            String currency,
            String description,
            String idempotencyKey);

    InvoiceResult getInvoice(String invoiceId, String idempotencyKey);

    record CustomerResult(String id) {}

    record SetupIntentResult(String id, String clientSecret) {}

    record PaymentMethodResult(String id, String type, String brand, String last4) {}

    record PaymentResult(String id, String status, boolean succeeded, String failureReason) {}

    record InvoiceResult(String id, long amount, String currency, String status) {}
}
