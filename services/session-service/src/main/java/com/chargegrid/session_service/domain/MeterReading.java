package com.chargegrid.session_service.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meter_readings")
public class MeterReading {
    @Id private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal kwh;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected MeterReading() {}

    public MeterReading(UUID sessionId, BigDecimal kwh, Instant at) {
        id = UUID.randomUUID();
        this.sessionId = sessionId;
        this.kwh = kwh;
        recordedAt = at;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public BigDecimal getKwh() {
        return kwh;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
