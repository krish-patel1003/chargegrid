package com.chargegrid.discovery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chargegrid.discovery.repository.ConnectorRepository;
import com.chargegrid.discovery.repository.StationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class StationServiceTest {
    private final StationRepository stations = mock(StationRepository.class);
    private final ConnectorRepository connectors = mock(ConnectorRepository.class);
    private final StationService service = new StationService(stations, connectors);

    @Test
    void nearbyPreservesRepositoryDistanceOrder() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        StationRepository.NearbyStation a = result(first, 1.2);
        StationRepository.NearbyStation b = result(second, 4.8);
        when(stations.findNearby(33.76, -118.19, 10, "CCS", true)).thenReturn(List.of(a, b));
        when(connectors.findByStationId(first)).thenReturn(List.of());
        when(connectors.findByStationId(second)).thenReturn(List.of());

        assertThat(service.nearby(33.76, -118.19, 10, "CCS", true))
                .extracting(StationService.StationResponse::id)
                .containsExactly(first, second);
    }

    @Test
    void missingStationIsNotFound() {
        UUID id = UUID.randomUUID();
        when(stations.findById(id)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Station not found");
    }

    private static StationRepository.NearbyStation result(UUID id, double distance) {
        StationRepository.NearbyStation result = mock(StationRepository.NearbyStation.class);
        when(result.getId()).thenReturn(id);
        when(result.getName()).thenReturn("Station");
        when(result.getAddress()).thenReturn("Long Beach");
        when(result.getDistanceKm()).thenReturn(distance);
        return result;
    }
}
