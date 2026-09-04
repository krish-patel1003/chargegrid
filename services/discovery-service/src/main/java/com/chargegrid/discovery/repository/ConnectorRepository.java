package com.chargegrid.discovery.repository;

import com.chargegrid.discovery.domain.Connector;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectorRepository extends JpaRepository<Connector, UUID> {
    List<Connector> findByStationId(UUID stationId);
}
