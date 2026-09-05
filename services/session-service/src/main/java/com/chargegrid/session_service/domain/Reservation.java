package com.chargegrid.session_service.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class Reservation {
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

    @Column(nullable = false)
    private boolean active;

    protected Reservation() {}

    public Reservation(
            String ownerId, String stationId, String connectorId, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.stationId = stationId;
        this.connectorId = connectorId;
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

    public void expire() {
        status = Status.EXPIRED;
        active = false;
    }

    public void cancel() {
        status = Status.CANCELLED;
        active = false;
    }

    public void started() {
        status = Status.STARTED;
        active = false;
    }
}
