package com.chargegrid.session_service.repository;

import com.chargegrid.session_service.domain.MeterReading;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {
    List<MeterReading> findAllBySessionIdOrderByRecordedAtAsc(UUID id);
}
