package com.chargegrid.billing.repository;

import com.chargegrid.billing.domain.Invoice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);
}
