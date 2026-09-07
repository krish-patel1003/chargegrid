package com.chargegrid.session_service.catalog;

import com.chargegrid.session_service.web.ApiException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reads the station catalogue over HTTP.
 *
 * <p>The client is built explicitly rather than injected, so the timeouts are visible here and the
 * service does not depend on which {@code RestClient.Builder} auto-configuration happens to be on
 * the classpath. A reservation must not hang because the catalogue is slow.
 */
@Component
public class DiscoveryStationCatalog implements StationCatalog {

    private final RestClient client;

    public DiscoveryStationCatalog(
            @Value("${chargegrid.discovery-service.url}") String baseUrl,
            @Value("${chargegrid.discovery-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${chargegrid.discovery-service.read-timeout-ms:3000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.client = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
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
