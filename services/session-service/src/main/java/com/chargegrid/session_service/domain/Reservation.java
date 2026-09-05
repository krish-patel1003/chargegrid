package com.chargegrid.session_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class Reservation {

    /** A driver gets this many wrong start codes before the reservation locks them out. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    @Id private UUID id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "station_id", nullable = false)
    private String stationId;

    @Column(name = "connector_id", nullable = false)
    private String connectorId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Mirrors {@code status == ACTIVE}, and exists as its own column so the partial unique index
     * {@code uk_active_connector} can enforce "one live reservation per connector" in the database
     * rather than only in application code.
     */
    @Column(nullable = false)
    private boolean active;

    @Column(name = "start_code_hash", nullable = false)
    private String startCodeHash;

    /** Plaintext the charger's display shows. See V2__charging_codes_and_tariffs.sql. */
    @Column(name = "start_code_display", nullable = false)
    private String startCodeDisplay;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /** Tariff captured when the reservation was made, so later price changes do not apply. */
    @Column(name = "rate_per_kwh", nullable = false, precision = 19, scale = 6)
    private BigDecimal ratePerKwh;

    @Column(name = "session_id")
    private UUID sessionId;

    protected Reservation() {}

    public Reservation(
            String ownerId,
            String stationId,
            String connectorId,
            BigDecimal ratePerKwh,
            String startCodeHash,
            String startCodeDisplay,
            Instant now,
            Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.stationId = stationId;
        this.connectorId = connectorId;
        this.ratePerKwh = ratePerKwh;
        this.startCodeHash = startCodeHash;
        this.startCodeDisplay = startCodeDisplay;
        this.status = Status.ACTIVE;
        this.active = true;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public enum Status {
        ACTIVE,
        EXPIRED,
        CANCELLED,
        STARTED
    }

    public UUID getId() {
        return id;
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

    public Status getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public String getStartCodeHash() {
        return startCodeHash;
    }

    public String getStartCodeDisplay() {
        return startCodeDisplay;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public BigDecimal getRatePerKwh() {
        return ratePerKwh;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public boolean isLockedOut() {
        return failedAttempts >= MAX_FAILED_ATTEMPTS;
    }

    public void recordFailedAttempt() {
        failedAttempts++;
    }

    public void expire() {
        status = Status.EXPIRED;
        active = false;
    }

    public void cancel() {
        status = Status.CANCELLED;
        active = false;
    }

    public void started(UUID sessionId) {
        this.status = Status.STARTED;
        this.active = false;
        this.sessionId = sessionId;
    }
}
