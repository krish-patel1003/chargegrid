package com.chargegrid.session_service.service;

import com.chargegrid.session_service.catalog.StationCatalog;
import com.chargegrid.session_service.domain.ChargingSession;
import com.chargegrid.session_service.domain.MeterReading;
import com.chargegrid.session_service.domain.Reservation;
import com.chargegrid.session_service.dto.Dtos;
import com.chargegrid.session_service.lock.ReservationLock;
import com.chargegrid.session_service.outbox.OutboxWriter;
import com.chargegrid.session_service.repository.ChargingSessionRepository;
import com.chargegrid.session_service.repository.MeterReadingRepository;
import com.chargegrid.session_service.repository.ReservationRepository;
import com.chargegrid.session_service.web.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Owns the reservation and charging lifecycle: hold a connector, hand the driver a start code,
 * meter energy while charging, and settle the session on a valid stop code.
 */
@Service
public class SessionService {

    private static final Duration RESERVATION_WINDOW = Duration.ofMinutes(10);
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(2);

    private final ReservationRepository reservations;
    private final ChargingSessionRepository sessions;
    private final MeterReadingRepository readings;
    private final ReservationLock lock;
    private final StationCatalog catalog;
    private final AccessCodes codes;
    private final OutboxWriter outbox;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public SessionService(
            ReservationRepository reservations,
            ChargingSessionRepository sessions,
            MeterReadingRepository readings,
            ReservationLock lock,
            StationCatalog catalog,
            AccessCodes codes,
            OutboxWriter outbox,
            TransactionTemplate transactions,
            Clock clock) {
        this.reservations = reservations;
        this.sessions = sessions;
        this.readings = readings;
        this.lock = lock;
        this.catalog = catalog;
        this.codes = codes;
        this.outbox = outbox;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * Holds a connector for the caller. The distributed lock serialises concurrent attempts on the
     * same connector; the {@code uk_active_connector} partial unique index is the backstop if the
     * lock is ever unavailable.
     */
    public Dtos.ReservationView reserve(String owner, String connectorId) {
        StationCatalog.ConnectorDetail connector = catalog.connector(connectorId);
        if (!connector.available()) {
            throw new ApiException(HttpStatus.CONFLICT, "connector is out of service");
        }
        try (AutoCloseable ignored = lock.acquire(connectorId, LOCK_TIMEOUT)) {
            // The transaction is opened and committed inside the lock. Annotating
            // this method instead would commit after the lock is released, letting
            // a second driver read "no active reservation" and insert one too.
            Reservation saved =
                    transactions.execute(
                            status -> insertReservation(owner, connectorId, connector));
            return Dtos.ReservationView.of(saved);
        } catch (ApiException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            // uk_active_connector rejected it. The lock makes this rare rather than
            // impossible, and the index is what actually guarantees the invariant.
            throw new ApiException(HttpStatus.CONFLICT, "connector is already reserved");
        } catch (IllegalStateException e) {
            // The lock timed out: another driver is mid-reservation on this connector.
            throw new ApiException(HttpStatus.CONFLICT, "connector is busy");
        } catch (Exception e) {
            throw new IllegalStateException("failed to reserve connector " + connectorId, e);
        }
    }

    private Reservation insertReservation(
            String owner, String connectorId, StationCatalog.ConnectorDetail connector) {
        if (reservations.existsByConnectorIdAndActiveTrue(connectorId)) {
            throw new ApiException(HttpStatus.CONFLICT, "connector is already reserved");
        }
        String code = codes.generate();
        Instant now = Instant.now(clock);
        return reservations.save(
                new Reservation(
                        owner,
                        connector.stationId(),
                        connectorId,
                        connector.ratePerKwh(),
                        codes.hash(code),
                        code,
                        now,
                        now.plus(RESERVATION_WINDOW)));
    }

    @Transactional
    public Dtos.ReservationView reservation(UUID id, String owner) {
        return Dtos.ReservationView.of(loadReservation(id, owner));
    }

    @Transactional
    public List<Dtos.ReservationView> reservations(String owner) {
        return reservations.findAllByOwnerId(owner).stream()
                .peek(this::expireIfElapsed)
                .map(Dtos.ReservationView::of)
                .toList();
    }

    @Transactional
    public Dtos.ReservationView cancel(UUID id, String owner) {
        Reservation reservation = loadReservation(id, owner);
        if (reservation.getStatus() == Reservation.Status.ACTIVE) {
            reservation.cancel();
            reservations.save(reservation);
        }
        return Dtos.ReservationView.of(reservation);
    }

    /** Exchanges a valid start code for a charging session. */
    @Transactional
    public Dtos.SessionView verifyStart(UUID reservationId, String owner, String code) {
        Reservation reservation = loadReservation(reservationId, owner);
        if (reservation.getStatus() == Reservation.Status.EXPIRED) {
            throw new ApiException(HttpStatus.GONE, "reservation has expired");
        }
        if (reservation.getStatus() != Reservation.Status.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "reservation cannot be started");
        }
        if (reservation.isLockedOut()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too many failed attempts");
        }
        if (!codes.matches(code, reservation.getStartCodeHash())) {
            reservation.recordFailedAttempt();
            reservations.save(reservation);
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid start code");
        }
        String stopCode = codes.generate();
        ChargingSession session =
                sessions.save(
                        new ChargingSession(
                                reservation, codes.hash(stopCode), stopCode, Instant.now(clock)));
        reservation.started(session.getId());
        reservations.save(reservation);
        return Dtos.SessionView.of(session);
    }

    @Transactional
    public Dtos.SessionView session(UUID id, String owner) {
        return Dtos.SessionView.of(loadSession(id, owner));
    }

    @Transactional
    public List<Dtos.SessionView> sessions(String owner) {
        return sessions.findAllByOwnerIdOrderByStartedAtDesc(owner).stream()
                .map(Dtos.SessionView::of)
                .toList();
    }

    /** Ends a session on a valid stop code and publishes the event billing settles from. */
    @Transactional
    public Dtos.SessionView verifyStop(UUID id, String owner, String code) {
        ChargingSession session = loadSession(id, owner);
        if (session.getStatus() != ChargingSession.Status.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "session is already stopped");
        }
        if (!codes.matches(code, session.getStopCodeHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid stop code");
        }
        session.complete(Instant.now(clock));
        ChargingSession completed = sessions.save(session);
        publishCompleted(completed);
        return Dtos.SessionView.of(completed);
    }

    /**
     * Records energy delivered by the charger. Operator-facing: only the hardware knows how many
     * kilowatt-hours actually flowed, so drivers cannot call this.
     */
    @Transactional
    public Dtos.SessionView meter(UUID id, BigDecimal kwh) {
        if (kwh == null || kwh.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "kwh must be positive");
        }
        ChargingSession session =
                sessions.findById(id)
                        .orElseThrow(
                                () -> new ApiException(HttpStatus.NOT_FOUND, "session not found"));
        if (session.getStatus() != ChargingSession.Status.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "session is already stopped");
        }
        session.addEnergy(kwh);
        readings.save(new MeterReading(id, kwh, Instant.now(clock)));
        return Dtos.SessionView.of(sessions.save(session));
    }

    @Transactional
    public Dtos.SimulatorView simulator(String stationId) {
        List<Dtos.SimulatorReservation> stationReservations =
                reservations.findAllByStationId(stationId).stream()
                        .peek(this::expireIfElapsed)
                        .map(Dtos.SimulatorReservation::of)
                        .toList();
        List<Dtos.SimulatorSession> stationSessions =
                sessions.findAllByStationId(stationId).stream()
                        .map(Dtos.SimulatorSession::of)
                        .toList();
        return new Dtos.SimulatorView(stationId, stationReservations, stationSessions);
    }

    private Reservation loadReservation(UUID id, String owner) {
        Reservation reservation =
                reservations
                        .findByIdAndOwnerId(id, owner)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "reservation not found"));
        expireIfElapsed(reservation);
        return reservation;
    }

    private ChargingSession loadSession(UUID id, String owner) {
        return sessions.findByIdAndOwnerId(id, owner)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "session not found"));
    }

    private void expireIfElapsed(Reservation reservation) {
        if (reservation.getStatus() == Reservation.Status.ACTIVE
                && !reservation.getExpiresAt().isAfter(Instant.now(clock))) {
            reservation.expire();
            reservations.save(reservation);
        }
    }

    /**
     * Records the completion event in the same transaction as the completed session, so the two
     * cannot diverge. The relay moves it to the broker afterwards.
     */
    private void publishCompleted(ChargingSession session) {
        outbox.write(
                "charging_session",
                session.getId().toString(),
                "ChargingSessionCompleted",
                Map.of(
                        "sessionId", session.getId().toString(),
                        "ownerId", session.getOwnerId(),
                        "stationId", session.getStationId(),
                        "connectorId", session.getConnectorId(),
                        "energyKwh", session.getEnergyKwh(),
                        "cost", session.getCost(),
                        "currency", "usd"));
    }
}
