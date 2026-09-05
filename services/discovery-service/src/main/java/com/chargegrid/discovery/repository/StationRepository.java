package com.chargegrid.discovery.repository;

import com.chargegrid.discovery.domain.Station;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StationRepository extends JpaRepository<Station, UUID> {
    interface NearbyStation {
        UUID getId();

        String getName();

        String getAddress();

        double getLatitude();

        double getLongitude();

        double getDistanceKm();
    }

    @Query(
            value =
                    """
        SELECT s.id AS id, s.name AS name, s.address AS address,
               s.latitude AS latitude, s.longitude AS longitude,
               ST_Distance(s.location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) / 1000.0 AS distanceKm
        FROM stations s
        WHERE ST_DWithin(s.location,
               ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
               :radiusKm * 1000.0)
          AND (:connectorType IS NULL OR EXISTS (
               SELECT 1 FROM connectors c WHERE c.station_id = s.id AND c.connector_type = :connectorType))
          AND (:available IS NULL OR EXISTS (
               SELECT 1 FROM connectors c WHERE c.station_id = s.id AND c.available = :available))
        ORDER BY distanceKm, s.id
        """,
            nativeQuery = true)
    List<NearbyStation> findNearby(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusKm") double radiusKm,
            @Param("connectorType") String connectorType,
            @Param("available") Boolean available);
}
