package com.chargegrid.station.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() { }
    public record StationView(String id, String name, double latitude, double longitude, List<ConnectorView> connectors) { }
    public record ConnectorView(String id, String type, double ratePerKwh) { }
    public record ReservationRequest(@NotBlank String connectorId) { }
    public record CodeRequest(@NotBlank @Pattern(regexp = "\\d{6}") String code) { }
    public record MeterRequest(@NotNull @DecimalMin("0.01") Double kwh) { }
    public record ReservationView(String id, String connectorId, String ownerId, String status, Instant expiresAt, String sessionId) { }
    public record SessionView(String id, String reservationId, String connectorId, String ownerId, String status,
                              Instant startedAt, double meterKwh, BigDecimal cost) { }
    public record SimulatorView(String stationId, List<SimulatorConnector> connectors, List<SimulatorReservation> reservations,
                                List<SimulatorSession> sessions) { }
    public record SimulatorConnector(String id, String type, double ratePerKwh) { }
    public record SimulatorReservation(String id, String connectorId, String ownerId, String status, String startCode) { }
    public record SimulatorSession(String id, String connectorId, String ownerId, String stopCode, double meterKwh, BigDecimal cost) { }
}
