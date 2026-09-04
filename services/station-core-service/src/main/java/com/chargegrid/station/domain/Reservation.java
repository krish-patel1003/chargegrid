package com.chargegrid.station.domain;

import java.time.Instant;

public class Reservation {
    public enum Status { RESERVED, STARTED, EXPIRED, COMPLETED }
    public final String id;
    public final String connectorId;
    public final String ownerId;
    public final Instant expiresAt;
    public final String startCodeHash;
    public final String startCode;
    public Status status = Status.RESERVED;
    public int failedAttempts;
    public String sessionId;
    public Reservation(String id, String connectorId, String ownerId, Instant expiresAt, String startCodeHash, String startCode) {
        this.id = id; this.connectorId = connectorId; this.ownerId = ownerId; this.expiresAt = expiresAt;
        this.startCodeHash = startCodeHash; this.startCode = startCode;
    }
}
