package com.chargegrid.billing.customer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingCustomerRepository extends JpaRepository<BillingCustomer, String> {
    Optional<BillingCustomer> findByStripeCustomerId(String stripeCustomerId);
}
