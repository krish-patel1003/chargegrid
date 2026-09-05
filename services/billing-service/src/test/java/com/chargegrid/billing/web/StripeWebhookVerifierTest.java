package com.chargegrid.billing.web;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class StripeWebhookVerifierTest {
    @Test
    void rejectsMissingSignatureWhenSecretConfigured() {
        var verifier = new StripeWebhookVerifier("whsec_test");
        assertThrows(ResponseStatusException.class, () -> verifier.verify("{}", null));
    }

    @Test
    void acceptsUnsignedPayloadOnlyWhenVerificationIsNotConfigured() {
        var verifier = new StripeWebhookVerifier("");
        verifier.verify("{\"id\":\"evt_test\",\"object\":\"event\"}", null);
    }
}
