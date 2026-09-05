package com.chargegrid.session_service.dto;

import com.chargegrid.session_service.domain.ChargingSession;
import com.chargegrid.session_service.domain.Reservation;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Dtos {

    private Dtos() {}

    public record ReserveRequest(@NotBlank String connectorId) {}

    public record CodeRequest(@NotBlank @Pattern(regexp = "\\d{6}") String code) {}

    public record MeterRequest(@NotNull @DecimalMin("0.01") BigDecimal kwh) {}

    public record ReservationView(
            String id,
            String stationId,
            String connectorId,
            String ownerId,
            String status,
            Instant expiresAt,
            BigDecimal ratePerKwh,
            String sessionId) {

        public static ReservationView of(Reservation reservation) {
            return new ReservationView(
                    reservation.getId().toString(),
                    reservation.getStationId(),
                    reservation.getConnectorId(),
                    reservation.getOwnerId(),
                    reservation.getStatus().name(),
                    reservation.getExpiresAt(),
                    reservation.getRatePerKwh(),
                    reservation.getSessionId() == null
                            ? null
                            : reservation.getSessionId().toString());
        }
    }

    public record SessionView(
            String id,
            String reservationId,
            String stationId,
            String connectorId,
            String ownerId,
            String status,
            Instant startedAt,
            Instant completedAt,
            BigDecimal meterKwh,
            BigDecimal cost) {

        public static SessionView of(ChargingSession session) {
            return new SessionView(
                    session.getId().toString(),
                    session.getReservationId().toString(),
                    session.getStationId(),
                    session.getConnectorId(),
                    session.getOwnerId(),
                    session.getStatus().name(),
                    session.getStartedAt(),
                    session.getCompletedAt(),
                    session.getEnergyKwh(),
                    session.getCost());
        }
    }

    /**
     * Operator view of one station. Includes the plaintext codes, because it represents the
     * charger's own display rather than anything a driver can reach.
     */
    public record SimulatorView(
            String stationId,
            List<SimulatorReservation> reservations,
            List<SimulatorSession> sessions) {}

    public record SimulatorReservation(
            String id, String connectorId, String ownerId, String status, String startCode) {

        public static SimulatorReservation of(Reservation reservation) {
            return new SimulatorReservation(
                    reservation.getId().toString(),
                    reservation.getConnectorId(),
                    reservation.getOwnerId(),
                    reservation.getStatus().name(),
                    reservation.getStartCodeDisplay());
        }
    }

    public record SimulatorSession(
            String id,
            String connectorId,
            String ownerId,
            String status,
            String stopCode,
            BigDecimal meterKwh,
            BigDecimal cost) {

        public static SimulatorSession of(ChargingSession session) {
            return new SimulatorSession(
                    session.getId().toString(),
                    session.getConnectorId(),
                    session.getOwnerId(),
                    session.getStatus().name(),
                    session.getStopCodeDisplay(),
                    session.getEnergyKwh(),
                    session.getCost());
        }
    }
}
