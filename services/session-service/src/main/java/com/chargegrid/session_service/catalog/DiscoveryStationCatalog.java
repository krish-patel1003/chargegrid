package com.chargegrid.session_service.catalog;

import com.chargegrid.session_service.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads the station catalogue over HTTP. Connect and read timeouts come from the {@code
 * spring.http.client.*} properties, so a slow catalogue cannot hold a reservation request open
 * indefinitely.
 */
@Component
public class DiscoveryStationCatalog implements StationCatalog {

    private final RestClient client;

    public DiscoveryStationCatalog(
            RestClient.Builder builder,
            @Value("${chargegrid.discovery-service.url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    public ConnectorDetail connector(String connectorId) {
        try {
            ConnectorDetail detail =
                    client.get()
                            .uri("/api/connectors/{id}", connectorId)
                            .retrieve()
                            .onStatus(
                                    status -> status.value() == HttpStatus.NOT_FOUND.value(),
                                    (request, response) -> {
                                        throw new ApiException(
                                                HttpStatus.NOT_FOUND, "connector not found");
                                    })
                            .body(ConnectorDetail.class);
            if (detail == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "connector not found");
            }
            return detail;
        } catch (ApiException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE, "station catalogue is unavailable");
        }
    }
}
