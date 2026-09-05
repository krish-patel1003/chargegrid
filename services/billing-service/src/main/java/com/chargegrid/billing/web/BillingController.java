package com.chargegrid.billing.web;

import com.chargegrid.billing.gateway.BillingGateway;
import com.chargegrid.billing.service.InvoiceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
    private final BillingGateway gateway;
    private final InvoiceService invoices;
    private final StripeWebhookVerifier verifier;

    public BillingController(
            BillingGateway gateway, InvoiceService invoices, StripeWebhookVerifier verifier) {
        this.gateway = gateway;
        this.invoices = invoices;
        this.verifier = verifier;
    }

    @PostMapping("/customers")
    public BillingGateway.CustomerResult customer(@Valid @RequestBody CustomerRequest r) {
        return gateway.createCustomer(r.userId(), r.email(), r.name());
    }

    @PostMapping("/setup-intents")
    public BillingGateway.SetupIntentResult setup(@Valid @RequestBody SetupRequest r) {
        return gateway.createSetupIntent(r.customerId());
    }

    @GetMapping("/payment-method")
    public BillingGateway.PaymentMethodResult payment(@RequestParam String paymentMethodId) {
        return gateway.getPaymentMethod(paymentMethodId);
    }

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<String> webhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody String payload) {
        verifier.verify(payload, signature);
        return ResponseEntity.ok("received");
    }

    @GetMapping("/invoices/{invoiceId}")
    public InvoiceResponse invoice(@PathVariable String invoiceId) {
        var i = invoices.get(invoiceId);
        return new InvoiceResponse(
                i.getId(),
                i.getAmount(),
                i.getCurrency(),
                i.getStatus().name(),
                i.getStripeInvoiceId(),
                i.getIdempotencyKey());
    }

    public record CustomerRequest(
            @NotBlank String userId, @NotBlank @Email String email, @NotBlank String name) {}

    public record SetupRequest(@NotBlank String customerId) {}

    public record InvoiceResponse(
            String id,
            long amount,
            String currency,
            String status,
            String stripeInvoiceId,
            String idempotencyKey) {}
}
