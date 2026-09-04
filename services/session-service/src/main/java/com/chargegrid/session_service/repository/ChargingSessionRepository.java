package com.chargegrid.session_service.repository;
import com.chargegrid.session_service.domain.ChargingSession; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ChargingSessionRepository extends JpaRepository<ChargingSession,UUID>{Optional<ChargingSession> findByIdAndOwnerId(UUID id,String ownerId); List<ChargingSession> findAllByOwnerId(String ownerId);}
