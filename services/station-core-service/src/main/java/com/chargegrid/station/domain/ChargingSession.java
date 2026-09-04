package com.chargegrid.station.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class ChargingSession {
    public final String id;
    public final String reservationId;
    public final String connectorId;
    public final String ownerId;
    public final String stopCode;
    public final String stopCodeHash;
    public final Instant startedAt;
    public double meterKwh;
    public boolean stopped;
    public ChargingSession(String id, String reservationId, String connectorId, String ownerId, String stopCode,
                           String stopCodeHash, Instant startedAt) {
        this.id = id; this.reservationId = reservationId; this.connectorId = connectorId; this.ownerId = ownerId;
        this.stopCode = stopCode; this.stopCodeHash = stopCodeHash; this.startedAt = startedAt;
    }
    public BigDecimal cost(double rate) { return BigDecimal.valueOf(meterKwh * rate).setScale(2, java.math.RoundingMode.HALF_UP); }
}
