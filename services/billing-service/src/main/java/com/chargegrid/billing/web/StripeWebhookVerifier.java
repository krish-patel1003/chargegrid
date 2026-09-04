package com.chargegrid.billing.web;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StripeWebhookVerifier {
    private final String secret;
    public StripeWebhookVerifier(@Value("${stripe.webhook-secret:}") String secret) { this.secret = secret; }
    public void verify(String payload, String signature) {
        if (secret.isBlank()) return;
        if (signature == null || signature.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Stripe signature");
        try { Webhook.constructEvent(payload, signature, secret); }
        catch (SignatureVerificationException | RuntimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature", e); }
    }
}
