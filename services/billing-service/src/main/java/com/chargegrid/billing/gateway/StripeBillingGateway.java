package com.chargegrid.billing.gateway;

import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.net.RequestOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeBillingGateway implements BillingGateway {
    private final String apiKey;
    public StripeBillingGateway(@Value("${stripe.secret-key:}") String apiKey) { this.apiKey = apiKey; Stripe.apiKey = apiKey; }
    private RequestOptions options(String key) { return RequestOptions.builder().setApiKey(apiKey).setIdempotencyKey(key).build(); }
    public CustomerResult createCustomer(String userId, String email, String name) {
        try { var p = CustomerCreateParams.builder().setEmail(email).setName(name).putMetadata("user_id", userId).build(); return new CustomerResult(Customer.create(p).getId()); }
        catch (Exception e) { throw new GatewayException(e); }
    }
    public SetupIntentResult createSetupIntent(String customerId) {
        try { var p = SetupIntentCreateParams.builder().setCustomer(customerId).addPaymentMethodType("card").build(); var x = SetupIntent.create(p); return new SetupIntentResult(x.getId(), x.getClientSecret()); }
        catch (Exception e) { throw new GatewayException(e); }
    }
    public PaymentMethodResult getPaymentMethod(String id) {
        try { PaymentMethod p = PaymentMethod.retrieve(id); var card = p.getCard(); return new PaymentMethodResult(p.getId(), p.getType(), card == null ? null : card.getBrand(), card == null ? null : card.getLast4()); }
        catch (Exception e) { throw new GatewayException(e); }
    }
    public InvoiceResult getInvoice(String id, String key) {
        try { Invoice i = Invoice.retrieve(id, options(key)); return new InvoiceResult(i.getId(), i.getAmountDue(), i.getCurrency(), i.getStatus()); }
        catch (Exception e) { throw new GatewayException(e); }
    }
    public static class GatewayException extends RuntimeException { public GatewayException(Throwable cause) { super(cause); } }
}
