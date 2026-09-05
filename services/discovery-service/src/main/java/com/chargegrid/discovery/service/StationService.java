package com.chargegrid.discovery.service;

import com.chargegrid.discovery.domain.Connector;
import com.chargegrid.discovery.domain.Station;
import com.chargegrid.discovery.repository.ConnectorRepository;
import com.chargegrid.discovery.repository.StationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StationService {
    private final StationRepository stationRepository;
    private final ConnectorRepository connectorRepository;

    public StationService(
            StationRepository stationRepository, ConnectorRepository connectorRepository) {
        this.stationRepository = stationRepository;
        this.connectorRepository = connectorRepository;
    }

    public List<StationResponse> nearby(
            double latitude,
            double longitude,
            double radiusKm,
            String connectorType,
            Boolean available) {
        return stationRepository
                .findNearby(latitude, longitude, radiusKm, connectorType, available)
                .stream()
                .map(
                        result ->
                                toResponse(
                                        result,
                                        connectorRepository.findByStationId(result.getId())))
                .toList();
    }

    public StationResponse get(UUID id) {
        Station station =
                stationRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Station not found"));
        return toResponse(station, connectorRepository.findByStationId(id), null);
    }

    public List<ConnectorResponse> connectors(UUID stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found");
        }
        return connectorRepository.findByStationId(stationId).stream()
                .map(StationService::toConnector)
                .toList();
    }

    /**
     * Resolves a single connector to the station that owns it and its current tariff.
     * session-service calls this when a driver reserves, so the reservation can capture the station
     * and the rate that applied at that moment.
     */
    public ConnectorDetail connector(UUID connectorId) {
        Connector connector =
                connectorRepository
                        .findById(connectorId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Connector not found"));
        return new ConnectorDetail(
                connector.getId(),
                connector.getStationId(),
                connector.getConnectorType(),
                connector.getPowerKw(),
                connector.isAvailable(),
                connector.getRatePerKwh());
    }

    private StationResponse toResponse(
            StationRepository.NearbyStation station, List<Connector> connectors) {
        return new StationResponse(
                station.getId(),
                station.getName(),
                station.getAddress(),
                station.getLatitude(),
                station.getLongitude(),
                station.getDistanceKm(),
                connectors.stream().map(StationService::toConnector).toList());
    }

    private StationResponse toResponse(
            Station station, List<Connector> connectors, Double distanceKm) {
        return new StationResponse(
                station.getId(),
                station.getName(),
                station.getAddress(),
                station.getLatitude(),
                station.getLongitude(),
                distanceKm,
                connectors.stream().map(StationService::toConnector).toList());
    }

    private static ConnectorResponse toConnector(Connector connector) {
        return new ConnectorResponse(
                connector.getId(),
                connector.getConnectorType(),
                connector.getPowerKw(),
                connector.isAvailable(),
                connector.getRatePerKwh());
    }

    public record StationResponse(
            UUID id,
            String name,
            String address,
            double latitude,
            double longitude,
            Double distanceKm,
            List<ConnectorResponse> connectors) {}

    public record ConnectorResponse(
            UUID id, String connectorType, int powerKw, boolean available, BigDecimal ratePerKwh) {}

    public record ConnectorDetail(
            UUID id,
            UUID stationId,
            String connectorType,
            int powerKw,
            boolean available,
            BigDecimal ratePerKwh) {}
}
