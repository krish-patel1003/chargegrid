package com.chargegrid.session_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chargegrid.session_service.catalog.StationCatalog;
import com.chargegrid.session_service.domain.ChargingSession;
import com.chargegrid.session_service.domain.Reservation;
import com.chargegrid.session_service.dto.Dtos;
import com.chargegrid.session_service.events.SessionCompleted;
import com.chargegrid.session_service.lock.InMemoryReservationLock;
import com.chargegrid.session_service.repository.ChargingSessionRepository;
import com.chargegrid.session_service.repository.MeterReadingRepository;
import com.chargegrid.session_service.repository.ReservationRepository;
import com.chargegrid.session_service.web.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

class SessionServiceTest {

    private static final String OWNER = "driver-a";
    private static final String CONNECTOR = "connector-1";
    private static final String STATION = "station-1";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final BigDecimal RATE = new BigDecimal("0.450000");

    @Mock private ReservationRepository reservations;
    @Mock private ChargingSessionRepository sessions;
    @Mock private MeterReadingRepository readings;
    @Mock private ApplicationEventPublisher events;

    private AccessCodes codes;
    private SessionService service;
    private Clock clock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        codes = new AccessCodes();
        service =
                new SessionService(
                        reservations,
                        sessions,
                        readings,
                        new InMemoryReservationLock(),
                        connectorId ->
                                new StationCatalog.ConnectorDetail(
                                        connectorId, STATION, "CCS", 150, true, RATE),
                        codes,
                        events,
                        clock);
        when(reservations.save(any())).thenAnswer(i -> i.getArgument(0));
        when(sessions.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void reserveCapturesStationAndTariffFromTheCatalogue() {
        when(reservations.existsByConnectorIdAndActiveTrue(CONNECTOR)).thenReturn(false);

        Dtos.ReservationView view = service.reserve(OWNER, CONNECTOR);

        assertEquals(STATION, view.stationId());
        assertEquals(RATE, view.ratePerKwh());
        assertEquals(Reservation.Status.ACTIVE.name(), view.status());
        assertEquals(NOW.plusSeconds(600), view.expiresAt());
    }

    @Test
    void reserveRejectsAConnectorThatIsAlreadyHeld() {
        when(reservations.existsByConnectorIdAndActiveTrue(CONNECTOR)).thenReturn(true);

        ApiException error =
                assertThrows(ApiException.class, () -> service.reserve(OWNER, CONNECTOR));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }

    @Test
    void reserveRejectsAConnectorThatIsOutOfService() {
        SessionService offline =
                new SessionService(
                        reservations,
                        sessions,
                        readings,
                        new InMemoryReservationLock(),
                        connectorId ->
                                new StationCatalog.ConnectorDetail(
                                        connectorId, STATION, "CCS", 150, false, RATE),
                        codes,
                        events,
                        clock);

        ApiException error =
                assertThrows(ApiException.class, () -> offline.reserve(OWNER, CONNECTOR));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
    }

    @Test
    void reservationExpiresOnceTheHoldWindowElapses() {
        Reservation reservation = reservation(NOW.minusSeconds(1200), NOW.minusSeconds(600));
        when(reservations.findByIdAndOwnerId(reservation.getId(), OWNER))
                .thenReturn(Optional.of(reservation));

        assertEquals(
                Reservation.Status.EXPIRED.name(),
                service.reservation(reservation.getId(), OWNER).status());
        verify(reservations).save(reservation);
    }

    @Test
    void reservationsAreIsolatedByOwner() {
        UUID id = UUID.randomUUID();
        when(reservations.findByIdAndOwnerId(id, "driver-b")).thenReturn(Optional.empty());

        ApiException error =
                assertThrows(ApiException.class, () -> service.reservation(id, "driver-b"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    @Test
    void verifyStartRejectsAWrongCodeAndCountsTheAttempt() {
        Reservation reservation = reservation(NOW, NOW.plusSeconds(600));
        when(reservations.findByIdAndOwnerId(reservation.getId(), OWNER))
                .thenReturn(Optional.of(reservation));

        ApiException error =
                assertThrows(
                        ApiException.class,
                        () -> service.verifyStart(reservation.getId(), OWNER, "000000"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals(1, reservation.getFailedAttempts());
        verify(sessions, never()).save(any());
    }

    @Test
    void verifyStartLocksOutAfterTooManyWrongCodes() {
        Reservation reservation = reservation(NOW, NOW.plusSeconds(600));
        when(reservations.findByIdAndOwnerId(reservation.getId(), OWNER))
                .thenReturn(Optional.of(reservation));
        for (int i = 0; i < Reservation.MAX_FAILED_ATTEMPTS; i++) {
            assertThrows(
                    ApiException.class,
                    () -> service.verifyStart(reservation.getId(), OWNER, "000000"));
        }

        ApiException error =
                assertThrows(
                        ApiException.class,
                        () -> service.verifyStart(reservation.getId(), OWNER, "000000"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatus());
    }

    @Test
    void verifyStartOnAnExpiredReservationIsGone() {
        Reservation reservation = reservation(NOW.minusSeconds(1200), NOW.minusSeconds(600));
        when(reservations.findByIdAndOwnerId(reservation.getId(), OWNER))
                .thenReturn(Optional.of(reservation));

        ApiException error =
                assertThrows(
                        ApiException.class,
                        () ->
                                service.verifyStart(
                                        reservation.getId(),
                                        OWNER,
                                        reservation.getStartCodeDisplay()));

        assertEquals(HttpStatus.GONE, error.getStatus());
    }

    @Test
    void verifyStartWithTheDisplayedCodeOpensASession() {
        Reservation reservation = reservation(NOW, NOW.plusSeconds(600));
        when(reservations.findByIdAndOwnerId(reservation.getId(), OWNER))
                .thenReturn(Optional.of(reservation));

        Dtos.SessionView session =
                service.verifyStart(reservation.getId(), OWNER, reservation.getStartCodeDisplay());

        assertEquals(ChargingSession.Status.ACTIVE.name(), session.status());
        assertEquals(STATION, session.stationId());
        assertEquals(Reservation.Status.STARTED, reservation.getStatus());
        assertNotNull(reservation.getSessionId());
    }

    @Test
    void meteringAccumulatesEnergyAndPricesItAtTheCapturedRate() {
        ChargingSession session = session();
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

        service.meter(session.getId(), new BigDecimal("3.5"));
        Dtos.SessionView view = service.meter(session.getId(), new BigDecimal("1.0"));

        assertEquals(new BigDecimal("4.5"), view.meterKwh());
        assertEquals(new BigDecimal("2.03"), view.cost());
        verify(readings, times(2)).save(any());
    }

    @Test
    void meteringRejectsNonPositiveReadings() {
        ChargingSession session = session();
        when(sessions.findById(session.getId())).thenReturn(Optional.of(session));

        ApiException error =
                assertThrows(
                        ApiException.class, () -> service.meter(session.getId(), BigDecimal.ZERO));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void verifyStopRejectsAWrongCode() {
        ChargingSession session = session();
        when(sessions.findByIdAndOwnerId(session.getId(), OWNER)).thenReturn(Optional.of(session));

        ApiException error =
                assertThrows(
                        ApiException.class,
                        () -> service.verifyStop(session.getId(), OWNER, "000000"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(events, never()).publishEvent(any(SessionCompleted.class));
    }

    @Test
    void verifyStopCompletesTheSessionAndPublishesExactlyOnce() {
        ChargingSession session = session();
        when(sessions.findByIdAndOwnerId(session.getId(), OWNER)).thenReturn(Optional.of(session));

        Dtos.SessionView stopped =
                service.verifyStop(session.getId(), OWNER, session.getStopCodeDisplay());

        assertEquals(ChargingSession.Status.COMPLETED.name(), stopped.status());
        ApiException replay =
                assertThrows(
                        ApiException.class,
                        () ->
                                service.verifyStop(
                                        session.getId(), OWNER, session.getStopCodeDisplay()));
        assertEquals(HttpStatus.CONFLICT, replay.getStatus());
        verify(events, times(1)).publishEvent(any(SessionCompleted.class));
    }

    @Test
    void simulatorListsWhatIsLiveAtOneStation() {
        Reservation reservation = reservation(NOW, NOW.plusSeconds(600));
        ChargingSession session = session();
        when(reservations.findAllByStationId(STATION)).thenReturn(List.of(reservation));
        when(sessions.findAllByStationId(STATION)).thenReturn(List.of(session));

        Dtos.SimulatorView view = service.simulator(STATION);

        assertEquals(reservation.getStartCodeDisplay(), view.reservations().get(0).startCode());
        assertEquals(session.getStopCodeDisplay(), view.sessions().get(0).stopCode());
    }

    private Reservation reservation(Instant createdAt, Instant expiresAt) {
        String code = codes.generate();
        return new Reservation(
                OWNER, STATION, CONNECTOR, RATE, codes.hash(code), code, createdAt, expiresAt);
    }

    private ChargingSession session() {
        String stopCode = codes.generate();
        return new ChargingSession(
                reservation(NOW, NOW.plusSeconds(600)), codes.hash(stopCode), stopCode, NOW);
    }
}
