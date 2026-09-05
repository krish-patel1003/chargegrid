package com.chargegrid.session_service.lock;

import java.time.Duration;

public interface ReservationLock {
    AutoCloseable acquire(String connectorId, Duration timeout);
}
