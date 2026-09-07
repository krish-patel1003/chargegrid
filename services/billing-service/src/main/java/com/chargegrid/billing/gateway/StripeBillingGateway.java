package com.chargegrid.billing.gateway;

import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeBillingGateway implements BillingGateway {
    private final String apiKey;

    public StripeBillingGateway(@Value("${stripe.secret-key:}") String apiKey) {
        this.apiKey = apiKey;
        Stripe.apiKey = apiKey;
    }

    private RequestOptions options(String key) {
        return RequestOptions.builder().setApiKey(apiKey).setIdempotencyKey(key).build();
    }

    public CustomerResult createCustomer(String userId, String email, String name) {
        try {
            var p =
                    CustomerCreateParams.builder()
                            .setEmail(email)
                            .setName(name)
                            .putMetadata("user_id", userId)
                            .build();
            return new CustomerResult(Customer.create(p).getId());
        } catch (Exception e) {
            throw new GatewayException(e);
        }
    }

    public SetupIntentResult createSetupIntent(String customerId) {
        try {
            var p =
                    SetupIntentCreateParams.builder()
                            .setCustomer(customerId)
                            .addPaymentMethodType("card")
                            .build();
            var x = SetupIntent.create(p);
            return new SetupIntentResult(x.getId(), x.getClientSecret());
        } catch (Exception e) {
            throw new GatewayException(e);
        }
    }

    public PaymentMethodResult getPaymentMethod(String id) {
        try {
            PaymentMethod p = PaymentMethod.retrieve(id);
            var card = p.getCard();
            return new PaymentMethodResult(
                    p.getId(),
                    p.getType(),
                    card == null ? null : card.getBrand(),
                    card == null ? null : card.getLast4());
        } catch (Exception e) {
            throw new GatewayException(e);
        }
    }

    public String paymentMethodFromSetupIntent(String setupIntentId) {
        try {
            return SetupIntent.retrieve(setupIntentId).getPaymentMethod();
        } catch (Exception e) {
            throw new GatewayException(e);
        }
    }

    public void setDefaultPaymentMethod(String customerId, String paymentMethodId) {
        try {
            PaymentMethod.retrieve(paymentMethodId)
                    .attach(
                            com.stripe.param.PaymentMethodAttachParams.builder()
                                    .setCustomer(customerId)
                                    .build());
            Customer.retrieve(customerId)
                    .update(
                            CustomerUpdateParams.builder()
                                    .setInvoiceSettings(
                                            CustomerUpdateParams.InvoiceSettings.builder()
                                                    .setDefaultPaymentMethod(paymentMethodId)
                                                    .build())
                                    .build());
        } catch (Exception e) {
            throw new GatewayException(e);
        }
    }

    public PaymentResult chargeOffSession(
            String customerId,
            String paymentMethodId,
            long amountMinorUnits,
            String currency,
            String description,
            String idempotencyKey) {
        try {
            PaymentIntent intent =
                    PaymentIntent.create(
                            PaymentIntentCreateParams.builder()
                                    .setCustomer(customerId)
                                    .setPaymentMethod(paymentMethodId)
                                    .setAmount(amountMinorUnits)
                                    .setCurrency(currency)
                                    .setDescription(description)
                                    // The driver has already driven away, so this is
                                    // a merchant-initiated charge against a card they
                                    // consented to being billed on.
                                    .setOffSession(true)
                                    .setConfirm(true)
                                    .build(),
                            options(idempotencyKey));
            boolean succeeded = "succeeded".equals(intent.getStatus());
            String failure =
                    intent.getLastPaymentError() == null
                            ? null
                            : intent.getLastPaymentError().getMessage();
            return new PaymentResult(intent.getId(), intent.getStatus(), succeeded, failure);
        } catch (com.stripe.exception.CardException e) {
            // A declined card is an outcome, not an outage: record it and move on.
            return new PaymentResult(
                    e.getStripeError() == null
                            ? null
                            : e.getStripeError().getPaymentIntent() == null
                                    ? null
                                    : e.getStripeError().getPaymentIntent().getId(),
                    "failed",
                    false,
                    e.getMessage());
        } catch (Exception e) {
            throw new GatewayException(e);
        }
    }

    public InvoiceResult getInvoice(String id, String key) {
        try {
            Invoice i = Invoice.retrieve(id, options(key));
            return new InvoiceResult(i.getId(), i.getAmountDue(), i.getCurrency(), i.getStatus());
        } catch (Exception e) {
            throw new GatewayException(e);
        }
    }

    public static class GatewayException extends RuntimeException {
        public GatewayException(Throwable cause) {
            super(cause);
        }
    }
}
