package com.chargegrid.notification_service.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, Long> {
    boolean existsByEventId(String eventId);
}
