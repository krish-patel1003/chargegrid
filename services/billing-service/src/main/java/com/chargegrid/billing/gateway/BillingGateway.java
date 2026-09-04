package com.chargegrid.billing.gateway;

public interface BillingGateway {
    CustomerResult createCustomer(String userId, String email, String name);
    SetupIntentResult createSetupIntent(String customerId);
    PaymentMethodResult getPaymentMethod(String paymentMethodId);
    InvoiceResult getInvoice(String invoiceId, String idempotencyKey);
    record CustomerResult(String id) {} record SetupIntentResult(String id, String clientSecret) {}
    record PaymentMethodResult(String id, String type, String brand, String last4) {}
    record InvoiceResult(String id, long amount, String currency, String status) {}
}
