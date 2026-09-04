package com.chargegrid.session_service.repository;
import com.chargegrid.session_service.domain.Reservation; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ReservationRepository extends JpaRepository<Reservation,UUID>{Optional<Reservation> findByIdAndOwnerId(UUID id,String ownerId); List<Reservation> findAllByOwnerId(String ownerId); boolean existsByConnectorIdAndActiveTrue(String connectorId);}
