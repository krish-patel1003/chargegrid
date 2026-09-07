package com.chargegrid.billing.service;

import com.chargegrid.billing.customer.BillingCustomer;
import com.chargegrid.billing.customer.BillingCustomerRepository;
import com.chargegrid.billing.gateway.BillingGateway;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saving a card, and remembering which one to charge later. */
@Service
public class PaymentMethodService {

    private final BillingCustomerRepository customers;
    private final BillingGateway gateway;
    private final Clock clock;

    public PaymentMethodService(
            BillingCustomerRepository customers, BillingGateway gateway, Clock clock) {
        this.customers = customers;
        this.gateway = gateway;
        this.clock = clock;
    }

    /**
     * Starts card capture. The Stripe customer is created once per driver and reused, so repeated
     * visits to the payment screen do not accumulate customers.
     */
    @Transactional
    public BillingGateway.SetupIntentResult beginCardSetup(
            String userId, String email, String name) {
        BillingCustomer customer = customerFor(userId, email, name);
        return gateway.createSetupIntent(customer.getStripeCustomerId());
    }

    /** Records the card the browser just confirmed, so later sessions can be charged. */
    @Transactional
    public SavedCard completeCardSetup(String userId, String setupIntentId) {
        BillingCustomer customer =
                customers
                        .findById(userId)
                        .orElseThrow(() -> new IllegalStateException("No billing customer"));
        String paymentMethodId = gateway.paymentMethodFromSetupIntent(setupIntentId);
        gateway.setDefaultPaymentMethod(customer.getStripeCustomerId(), paymentMethodId);
        BillingGateway.PaymentMethodResult card = gateway.getPaymentMethod(paymentMethodId);
        customer.saveCard(card.id(), card.brand(), card.last4(), Instant.now(clock));
        customers.save(customer);
        return new SavedCard(card.brand(), card.last4());
    }

    @Transactional(readOnly = true)
    public Optional<SavedCard> savedCard(String userId) {
        return customers
                .findById(userId)
                .filter(BillingCustomer::hasCard)
                .map(c -> new SavedCard(c.getCardBrand(), c.getCardLast4()));
    }

    @Transactional(readOnly = true)
    public Optional<BillingCustomer> billingCustomer(String userId) {
        return customers.findById(userId);
    }

    private BillingCustomer customerFor(String userId, String email, String name) {
        return customers
                .findById(userId)
                .orElseGet(
                        () -> {
                            var created = gateway.createCustomer(userId, email, name);
                            return customers.save(
                                    new BillingCustomer(userId, created.id(), Instant.now(clock)));
                        });
    }

    public record SavedCard(String brand, String last4) {}
}
