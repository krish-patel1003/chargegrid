package com.chargegrid.session_service.repository;
import com.chargegrid.session_service.domain.MeterReading; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface MeterReadingRepository extends JpaRepository<MeterReading,UUID>{List<MeterReading> findAllBySessionIdOrderByRecordedAtAsc(UUID id);}
