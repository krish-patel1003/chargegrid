package com.chargegrid.session_service.repository;

import com.chargegrid.session_service.domain.ChargingSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, UUID> {

    Optional<ChargingSession> findByIdAndOwnerId(UUID id, String ownerId);

    List<ChargingSession> findAllByOwnerIdOrderByStartedAtDesc(String ownerId);

    List<ChargingSession> findAllByStationId(String stationId);
}
