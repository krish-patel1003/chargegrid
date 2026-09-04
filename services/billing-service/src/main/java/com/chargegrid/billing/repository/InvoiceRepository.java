package com.chargegrid.billing.repository;

import com.chargegrid.billing.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);
}
