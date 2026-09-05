package com.chargegrid.session_service.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
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

    @Column(name = "connector_id", nullable = false)
    private String connectorId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal energyKwh = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    protected ChargingSession() {}

    public ChargingSession(Reservation r, Instant now) {
        id = UUID.randomUUID();
        reservationId = r.getId();
        ownerId = r.getOwnerId();
        connectorId = r.getConnectorId();
        startedAt = now;
        status = Status.ACTIVE;
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

    public Status getStatus() {
        return status;
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
