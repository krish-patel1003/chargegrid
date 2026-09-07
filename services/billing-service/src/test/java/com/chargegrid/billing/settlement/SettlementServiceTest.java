package com.chargegrid.billing.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chargegrid.billing.customer.BillingCustomer;
import com.chargegrid.billing.domain.Invoice;
import com.chargegrid.billing.domain.InvoiceStatus;
import com.chargegrid.billing.gateway.BillingGateway;
import com.chargegrid.billing.repository.InvoiceRepository;
import com.chargegrid.billing.service.PaymentMethodService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SettlementServiceTest {

    private static final String SESSION = "5b1f0a2e-0000-4000-8000-000000000009";
    private static final String USER = "driver-1";

    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final PaymentMethodService paymentMethods = mock(PaymentMethodService.class);
    private final BillingGateway gateway = mock(BillingGateway.class);
    private final SettlementService settlement =
            new SettlementService(invoices, paymentMethods, gateway);

    @BeforeEach
    void savedCardOnFile() {
        when(invoices.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoices.findBySessionId(SESSION)).thenReturn(Optional.empty());
        BillingCustomer customer = new BillingCustomer(USER, "cus_123", Instant.EPOCH);
        customer.saveCard("pm_123", "visa", "4242", Instant.EPOCH);
        when(paymentMethods.billingCustomer(USER)).thenReturn(Optional.of(customer));
    }

    @Test
    void chargesTheSavedCardInMinorUnits() {
        when(gateway.chargeOffSession(any(), any(), anyLong(), any(), any(), any()))
                .thenReturn(new BillingGateway.PaymentResult("pi_1", "succeeded", true, null));

        settlement.settle(SESSION, USER, new BigDecimal("3.89"), "usd");

        // $3.89 is 389 cents; sending 3.89 would charge four cents.
        verify(gateway)
                .chargeOffSession(
                        eq("cus_123"), eq("pm_123"), eq(389L), eq("usd"), anyString(), anyString());
        ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
        verify(invoices).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(saved.getValue().getStripePaymentIntentId()).isEqualTo("pi_1");
    }

    @Test
    void doesNotChargeTwiceForAReplayedEvent() {
        when(invoices.findBySessionId(SESSION))
                .thenReturn(Optional.of(Invoice.forSession(SESSION, USER, 389, "usd")));

        settlement.settle(SESSION, USER, new BigDecimal("3.89"), "usd");

        verify(gateway, never()).chargeOffSession(any(), any(), anyLong(), any(), any(), any());
        verify(invoices, never()).save(any());
    }

    @Test
    void carriesAnIdempotencyKeyDerivedFromTheSession() {
        when(gateway.chargeOffSession(any(), any(), anyLong(), any(), any(), any()))
                .thenReturn(new BillingGateway.PaymentResult("pi_1", "succeeded", true, null));

        settlement.settle(SESSION, USER, new BigDecimal("1.00"), "usd");

        // Two concurrent replays would reach Stripe with the same key, and Stripe
        // returns the original charge rather than making a second one.
        verify(gateway)
                .chargeOffSession(
                        any(),
                        any(),
                        anyLong(),
                        any(),
                        anyString(),
                        eq("chargegrid-session-" + SESSION));
    }

    @Test
    void recordsADeclineWithoutLosingTheDebt() {
        when(gateway.chargeOffSession(any(), any(), anyLong(), any(), any(), any()))
                .thenReturn(
                        new BillingGateway.PaymentResult("pi_2", "failed", false, "card declined"));

        settlement.settle(SESSION, USER, new BigDecimal("3.89"), "usd");

        ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
        verify(invoices).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(InvoiceStatus.FAILED);
        assertThat(saved.getValue().getFailureReason()).isEqualTo("card declined");
    }

    @Test
    void recordsTheDebtWhenNoCardIsOnFile() {
        when(paymentMethods.billingCustomer(USER)).thenReturn(Optional.empty());

        settlement.settle(SESSION, USER, new BigDecimal("3.89"), "usd");

        verify(gateway, never()).chargeOffSession(any(), any(), anyLong(), any(), any(), any());
        ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
        verify(invoices).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(InvoiceStatus.FAILED);
        assertThat(saved.getValue().getFailureReason()).isEqualTo("No saved payment method");
    }

    @Test
    void doesNotChargeAZeroCostSession() {
        settlement.settle(SESSION, USER, BigDecimal.ZERO, "usd");

        verify(gateway, never()).chargeOffSession(any(), any(), anyLong(), any(), any(), any());
        verify(invoices, never()).save(any());
    }
}
