package com.chargegrid.billing.web;

import com.chargegrid.billing.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reconciles what Stripe reports against what was recorded.
 *
 * <p>An off-session charge can settle or fail after the API call returns, so the webhook is the
 * authority on the final state, not the response to the original request.
 */
@Component
public class SettlementWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(SettlementWebhookHandler.class);

    private final InvoiceRepository invoices;
    private final ObjectMapper objectMapper;

    public SettlementWebhookHandler(InvoiceRepository invoices, ObjectMapper objectMapper) {
        this.invoices = invoices;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handle(String payload) {
        JsonNode event;
        try {
            event = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("Ignoring unparseable Stripe webhook");
            return;
        }
        String type = event.path("type").asString("");
        JsonNode intent = event.path("data").path("object");
        String intentId = intent.path("id").asString("");
        if (intentId.isEmpty()) {
            return;
        }

        invoices.findByStripePaymentIntentId(intentId)
                .ifPresent(
                        invoice -> {
                            switch (type) {
                                case "payment_intent.succeeded" -> invoice.paid(intentId);
                                case "payment_intent.payment_failed" ->
                                        invoice.failed(
                                                intentId,
                                                intent.path("last_payment_error")
                                                        .path("message")
                                                        .asString("declined"));
                                default -> {
                                    return;
                                }
                            }
                            invoices.save(invoice);
                            log.info("Stripe {} reconciled invoice {}", type, invoice.getId());
                        });
    }
}
