package com.chargegrid.session_service.service;

import com.chargegrid.session_service.catalog.StationCatalog;
import com.chargegrid.session_service.domain.ChargingSession;
import com.chargegrid.session_service.domain.MeterReading;
import com.chargegrid.session_service.domain.Reservation;
import com.chargegrid.session_service.dto.Dtos;
import com.chargegrid.session_service.lock.ReservationLock;
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
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the reservation and charging lifecycle: hold a connector, hand the driver a start code,
 * meter energy while charging, and settle the session on a valid stop code.
 */
@Service
@Transactional
public class SessionService {

    private static final Duration RESERVATION_WINDOW = Duration.ofMinutes(10);
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(2);

    private final ReservationRepository reservations;
    private final ChargingSessionRepository sessions;
    private final MeterReadingRepository readings;
    private final ReservationLock lock;
    private final StationCatalog catalog;
    private final AccessCodes codes;
    private final AmqpTemplate events;
    private final Clock clock;

    public SessionService(
            ReservationRepository reservations,
            ChargingSessionRepository sessions,
            MeterReadingRepository readings,
            ReservationLock lock,
            StationCatalog catalog,
            AccessCodes codes,
            AmqpTemplate events) {
        this(reservations, sessions, readings, lock, catalog, codes, events, Clock.systemUTC());
    }

    public SessionService(
            ReservationRepository reservations,
            ChargingSessionRepository sessions,
            MeterReadingRepository readings,
            ReservationLock lock,
            StationCatalog catalog,
            AccessCodes codes,
            AmqpTemplate events,
            Clock clock) {
        this.reservations = reservations;
        this.sessions = sessions;
        this.readings = readings;
        this.lock = lock;
        this.catalog = catalog;
        this.codes = codes;
        this.events = events;
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
            if (reservations.existsByConnectorIdAndActiveTrue(connectorId)) {
                throw new ApiException(HttpStatus.CONFLICT, "connector is already reserved");
            }
            String code = codes.generate();
            Instant now = Instant.now(clock);
            Reservation reservation =
                    new Reservation(
                            owner,
                            connector.stationId(),
                            connectorId,
                            connector.ratePerKwh(),
                            codes.hash(code),
                            code,
                            now,
                            now.plus(RESERVATION_WINDOW));
            return Dtos.ReservationView.of(reservations.save(reservation));
        } catch (ApiException e) {
            throw e;
        } catch (IllegalStateException e) {
            // The lock timed out: another driver is mid-reservation on this connector.
            throw new ApiException(HttpStatus.CONFLICT, "connector is busy");
        } catch (Exception e) {
            throw new IllegalStateException("failed to reserve connector " + connectorId, e);
        }
    }

    public Dtos.ReservationView reservation(UUID id, String owner) {
        return Dtos.ReservationView.of(loadReservation(id, owner));
    }

    public List<Dtos.ReservationView> reservations(String owner) {
        return reservations.findAllByOwnerId(owner).stream()
                .peek(this::expireIfElapsed)
                .map(Dtos.ReservationView::of)
                .toList();
    }

    public Dtos.ReservationView cancel(UUID id, String owner) {
        Reservation reservation = loadReservation(id, owner);
        if (reservation.getStatus() == Reservation.Status.ACTIVE) {
            reservation.cancel();
            reservations.save(reservation);
        }
        return Dtos.ReservationView.of(reservation);
    }

    /** Exchanges a valid start code for a charging session. */
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

    public Dtos.SessionView session(UUID id, String owner) {
        return Dtos.SessionView.of(loadSession(id, owner));
    }

    public List<Dtos.SessionView> sessions(String owner) {
        return sessions.findAllByOwnerIdOrderByStartedAtDesc(owner).stream()
                .map(Dtos.SessionView::of)
                .toList();
    }

    /** Ends a session on a valid stop code and publishes the event billing settles from. */
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

    private void publishCompleted(ChargingSession session) {
        events.convertAndSend(
                new EventEnvelope(
                        UUID.randomUUID(),
                        "session.completed",
                        1,
                        UUID.randomUUID(),
                        Instant.now(clock),
                        Map.of(
                                "sessionId", session.getId(),
                                "ownerId", session.getOwnerId(),
                                "energyKwh", session.getEnergyKwh(),
                                "cost", session.getCost())));
    }

    public record EventEnvelope(
            UUID eventId,
            String eventType,
            int eventVersion,
            UUID correlationId,
            Instant occurredAt,
            Object payload) {}
}
