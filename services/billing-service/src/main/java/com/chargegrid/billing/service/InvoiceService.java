package com.chargegrid.billing.service;

import com.chargegrid.billing.domain.*;
import com.chargegrid.billing.gateway.BillingGateway;
import com.chargegrid.billing.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {
    private final InvoiceRepository invoices;
    private final BillingGateway gateway;

    public InvoiceService(InvoiceRepository invoices, BillingGateway gateway) {
        this.invoices = invoices;
        this.gateway = gateway;
    }

    @Transactional
    public Invoice get(String id) {
        String key = "chargegrid-invoice-" + id;
        return invoices.findById(id)
                .orElseGet(
                        () -> {
                            var remote = gateway.getInvoice(id, key);
                            var invoice =
                                    new Invoice(
                                            id,
                                            "stripe",
                                            remote.amount(),
                                            remote.currency(),
                                            parse(remote.status()),
                                            remote.id());
                            return invoices.save(invoice);
                        });
    }

    private InvoiceStatus parse(String status) {
        try {
            return InvoiceStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return InvoiceStatus.OPEN;
        }
    }
}
