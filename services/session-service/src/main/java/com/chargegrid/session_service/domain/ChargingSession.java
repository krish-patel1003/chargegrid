package com.chargegrid.session_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "charging_sessions")
public class ChargingSession {
    @Id private UUID id;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private UUID reservationId;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "station_id", nullable = false)
    private String stationId;

    @Column(name = "connector_id", nullable = false)
    private String connectorId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal energyKwh = BigDecimal.ZERO;

    @Column(name = "rate_per_kwh", nullable = false, precision = 19, scale = 6)
    private BigDecimal ratePerKwh;

    @Column(name = "stop_code_hash", nullable = false)
    private String stopCodeHash;

    /** Plaintext the charger's display shows. See V2__charging_codes_and_tariffs.sql. */
    @Column(name = "stop_code_display", nullable = false)
    private String stopCodeDisplay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    protected ChargingSession() {}

    public ChargingSession(
            Reservation reservation, String stopCodeHash, String stopCodeDisplay, Instant now) {
        this.id = UUID.randomUUID();
        this.reservationId = reservation.getId();
        this.ownerId = reservation.getOwnerId();
        this.stationId = reservation.getStationId();
        this.connectorId = reservation.getConnectorId();
        this.ratePerKwh = reservation.getRatePerKwh();
        this.stopCodeHash = stopCodeHash;
        this.stopCodeDisplay = stopCodeDisplay;
        this.startedAt = now;
        this.status = Status.ACTIVE;
    }

    public enum Status {
        ACTIVE,
        COMPLETED
    }

    public UUID getId() {
        return id;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getStationId() {
        return stationId;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public BigDecimal getEnergyKwh() {
        return energyKwh;
    }

    public BigDecimal getRatePerKwh() {
        return ratePerKwh;
    }

    public String getStopCodeHash() {
        return stopCodeHash;
    }

    public String getStopCodeDisplay() {
        return stopCodeDisplay;
    }

    public Status getStatus() {
        return status;
    }

    /** Energy delivered so far, priced at the tariff captured when the driver reserved. */
    public BigDecimal getCost() {
        return energyKwh.multiply(ratePerKwh).setScale(2, RoundingMode.HALF_UP);
    }

    public void addEnergy(BigDecimal value) {
        energyKwh = energyKwh.add(value);
    }

    public void complete(Instant now) {
        if (status == Status.ACTIVE) {
            status = Status.COMPLETED;
            completedAt = now;
        }
    }
}
