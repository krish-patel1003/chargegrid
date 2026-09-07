package com.chargegrid.session_service.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Raised when a charging session settles. Published in-process first so the broker hop happens only
 * after the database transaction commits.
 */
public record SessionCompleted(
        UUID sessionId,
        String ownerId,
        String stationId,
        String connectorId,
        BigDecimal energyKwh,
        BigDecimal cost,
        Instant occurredAt) {}
