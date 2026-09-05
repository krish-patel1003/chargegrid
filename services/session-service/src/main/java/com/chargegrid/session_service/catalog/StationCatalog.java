package com.chargegrid.session_service.catalog;

import java.math.BigDecimal;

/**
 * Read model of the station catalogue owned by discovery-service. session-service depends on the
 * abstraction so the reservation flow can be tested without an HTTP round trip.
 */
public interface StationCatalog {

    /**
     * @throws com.chargegrid.session_service.web.ApiException 404 when the connector is unknown,
     *     503 when the catalogue cannot be reached.
     */
    ConnectorDetail connector(String connectorId);

    record ConnectorDetail(
            String id,
            String stationId,
            String connectorType,
            int powerKw,
            boolean available,
            BigDecimal ratePerKwh) {}
}
