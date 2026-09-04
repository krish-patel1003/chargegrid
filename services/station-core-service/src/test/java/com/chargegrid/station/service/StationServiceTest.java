package com.chargegrid.station.service;

import static org.junit.jupiter.api.Assertions.*;

import com.chargegrid.station.dto.Dtos;
import com.chargegrid.station.web.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class StationServiceTest {
    @Test
    void conflictingReservationsAreRejected() {
        StationService service = new StationService();
        service.reserve("connector-1", "driver-a");
        ApiException error = assertThrows(ApiException.class, () -> service.reserve("connector-1", "driver-b"));
        assertEquals(HttpStatus.CONFLICT, error.status);
    }

    @Test
    void invalidAndReusedStartCodesHaveExpectedStatuses() {
        StationService service = new StationService();
        Dtos.ReservationView reservation = service.reserve("connector-1", "driver-a");
        ApiException invalid = assertThrows(ApiException.class, () -> service.verifyStart(reservation.id(), "driver-a", "000000"));
        assertEquals(HttpStatus.BAD_REQUEST, invalid.status);
        String code = service.simulator("station-1").reservations().get(0).startCode();
        service.verifyStart(reservation.id(), "driver-a", code);
        ApiException reused = assertThrows(ApiException.class, () -> service.verifyStart(reservation.id(), "driver-a", code));
        assertEquals(HttpStatus.CONFLICT, reused.status);
    }

    @Test
    void resourcesAreIsolatedByOwner() {
        StationService service = new StationService();
        Dtos.ReservationView reservation = service.reserve("connector-1", "driver-a");
        ApiException error = assertThrows(ApiException.class, () -> service.reservation(reservation.id(), "driver-b"));
        assertEquals(HttpStatus.FORBIDDEN, error.status);
    }

    @Test
    void meterProducesRateBasedCost() {
        StationService service = new StationService();
        Dtos.ReservationView reservation = service.reserve("connector-1", "driver-a");
        String code = service.simulator("station-1").reservations().get(0).startCode();
        Dtos.SessionView session = service.verifyStart(reservation.id(), "driver-a", code);
        Dtos.SessionView metered = service.meter(session.id(), 2.0);
        assertEquals(2.0, metered.meterKwh());
        assertEquals("0.90", metered.cost().toPlainString());
    }

    @Test
    void stopCodeStopsOnlyTheOwningSession() {
        StationService service = new StationService();
        Dtos.ReservationView reservation = service.reserve("connector-1", "driver-a");
        String code = service.simulator("station-1").reservations().get(0).startCode();
        Dtos.SessionView session = service.verifyStart(reservation.id(), "driver-a", code);
        String stopCode = service.simulator("station-1").sessions().get(0).stopCode();
        ApiException ownerError = assertThrows(ApiException.class, () -> service.verifyStop(session.id(), "driver-b", stopCode));
        assertEquals(HttpStatus.FORBIDDEN, ownerError.status);
        assertEquals("STOPPED", service.verifyStop(session.id(), "driver-a", stopCode).status());
    }
}
