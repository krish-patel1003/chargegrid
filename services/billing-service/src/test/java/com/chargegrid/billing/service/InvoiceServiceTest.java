package com.chargegrid.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import com.chargegrid.billing.gateway.BillingGateway;
import com.chargegrid.billing.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;

class InvoiceServiceTest {
    @Test
    void usesStableInvoiceIdempotencyKeyWhenFetchingRemoteInvoice() {
        var repository = mock(InvoiceRepository.class);
        var gateway = mock(BillingGateway.class);
        var id = "in_test_123";
        when(repository.findById(id)).thenReturn(java.util.Optional.empty());
        when(gateway.getInvoice(id.toString(), "chargegrid-invoice-" + id))
                .thenReturn(new BillingGateway.InvoiceResult("in_1", 1200, "usd", "paid"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var invoice = new InvoiceService(repository, gateway).get(id);
        assertEquals("chargegrid-invoice-" + id, invoice.getIdempotencyKey());
        verify(gateway).getInvoice(id.toString(), "chargegrid-invoice-" + id);
    }
}
