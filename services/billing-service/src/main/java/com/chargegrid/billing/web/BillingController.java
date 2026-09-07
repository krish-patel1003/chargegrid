package com.chargegrid.billing.web;

import com.chargegrid.billing.gateway.BillingGateway;
import com.chargegrid.billing.service.InvoiceService;
import com.chargegrid.billing.service.PaymentMethodService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final PaymentMethodService paymentMethods;
    private final InvoiceService invoices;
    private final StripeWebhookVerifier verifier;
    private final SettlementWebhookHandler webhooks;

    public BillingController(
            PaymentMethodService paymentMethods,
            InvoiceService invoices,
            StripeWebhookVerifier verifier,
            SettlementWebhookHandler webhooks) {
        this.paymentMethods = paymentMethods;
        this.invoices = invoices;
        this.verifier = verifier;
        this.webhooks = webhooks;
    }

    /**
     * Starts card capture and hands the browser a client secret. The card itself is entered into
     * Stripe Elements and never reaches this service.
     */
    @PostMapping("/setup-intent")
    public SetupIntentResponse beginCardSetup(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody SetupIntentRequest request) {
        BillingGateway.SetupIntentResult intent =
                paymentMethods.beginCardSetup(caller(userId), request.email(), request.name());
        return new SetupIntentResponse(intent.id(), intent.clientSecret());
    }

    /** Records the card the browser confirmed, so later sessions can be charged. */
    @PostMapping("/payment-method")
    public PaymentMethodService.SavedCard completeCardSetup(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CompleteSetupRequest request) {
        return paymentMethods.completeCardSetup(caller(userId), request.setupIntentId());
    }

    @GetMapping("/payment-method")
    public ResponseEntity<PaymentMethodService.SavedCard> savedCard(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return paymentMethods
                .savedCard(caller(userId))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<String> webhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody String payload) {
        verifier.verify(payload, signature);
        webhooks.handle(payload);
        return ResponseEntity.ok("received");
    }

    @GetMapping("/invoices/{invoiceId}")
    public Map<String, Object> invoice(@PathVariable String invoiceId) {
        var i = invoices.get(invoiceId);
        return Map.of(
                "id", i.getId(),
                "amount", i.getAmount(),
                "currency", i.getCurrency(),
                "status", i.getStatus().name(),
                "idempotencyKey", i.getIdempotencyKey());
    }

    private static String caller(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "X-User-Id header is required");
        }
        return userId;
    }

    public record SetupIntentRequest(@NotBlank @Email String email, @NotBlank String name) {}

    public record CompleteSetupRequest(@NotBlank String setupIntentId) {}

    public record SetupIntentResponse(String setupIntentId, String clientSecret) {}
}
