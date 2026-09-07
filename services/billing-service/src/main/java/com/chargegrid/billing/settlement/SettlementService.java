package com.chargegrid.billing.settlement;

import com.chargegrid.billing.customer.BillingCustomer;
import com.chargegrid.billing.domain.Invoice;
import com.chargegrid.billing.gateway.BillingGateway;
import com.chargegrid.billing.repository.InvoiceRepository;
import com.chargegrid.billing.service.PaymentMethodService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Charges a driver's saved card for a completed session. */
@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final InvoiceRepository invoices;
    private final PaymentMethodService paymentMethods;
    private final BillingGateway gateway;

    public SettlementService(
            InvoiceRepository invoices,
            PaymentMethodService paymentMethods,
            BillingGateway gateway) {
        this.invoices = invoices;
        this.paymentMethods = paymentMethods;
        this.gateway = gateway;
    }

    /**
     * Settles one session.
     *
     * <p>Guarded twice against the at-least-once event stream: an invoice already recorded for the
     * session short-circuits, and the Stripe call carries an idempotency key derived from the
     * session id, so even a concurrent replay cannot charge twice.
     */
    @Transactional
    public void settle(String sessionId, String userId, BigDecimal cost, String currency) {
        Optional<Invoice> existing = invoices.findBySessionId(sessionId);
        if (existing.isPresent()) {
            log.debug("Session {} already settled as {}", sessionId, existing.get().getId());
            return;
        }

        long amount = toMinorUnits(cost);
        if (amount <= 0) {
            log.info("Session {} cost nothing; nothing to charge", sessionId);
            return;
        }

        Optional<BillingCustomer> customer = paymentMethods.billingCustomer(userId);
        if (customer.isEmpty() || !customer.get().hasCard()) {
            // No saved card. Record the debt rather than dropping it, so it is
            // visible and can be collected once the driver adds one.
            Invoice unpaid = Invoice.forSession(sessionId, userId, amount, currency);
            unpaid.failed(null, "No saved payment method");
            invoices.save(unpaid);
            log.warn("Session {} has no payment method on file for {}", sessionId, userId);
            return;
        }

        Invoice invoice = Invoice.forSession(sessionId, userId, amount, currency);
        BillingGateway.PaymentResult result =
                gateway.chargeOffSession(
                        customer.get().getStripeCustomerId(),
                        customer.get().getDefaultPaymentMethodId(),
                        amount,
                        currency,
                        "ChargeGrid session " + sessionId,
                        invoice.getIdempotencyKey());

        if (result.succeeded()) {
            invoice.paid(result.id());
        } else {
            invoice.failed(result.id(), result.failureReason());
        }
        invoices.save(invoice);
    }

    /** Stripe charges in the smallest currency unit, so dollars become cents. */
    private static long toMinorUnits(BigDecimal cost) {
        return cost.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }
}
