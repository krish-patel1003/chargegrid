package com.chargegrid.session_service.repository;

import com.chargegrid.session_service.domain.ChargingSession;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, UUID> {
    Optional<ChargingSession> findByIdAndOwnerId(UUID id, String ownerId);

    List<ChargingSession> findAllByOwnerId(String ownerId);
}
