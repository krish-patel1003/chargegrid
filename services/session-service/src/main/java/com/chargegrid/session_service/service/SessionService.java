package com.chargegrid.session_service.service;

import com.chargegrid.session_service.domain.*;
import com.chargegrid.session_service.lock.ReservationLock;
import com.chargegrid.session_service.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SessionService {
    private final ReservationRepository reservations;
    private final ChargingSessionRepository sessions;
    private final MeterReadingRepository readings;
    private final ReservationLock lock;
    private final AmqpTemplate events;
    private final Clock clock;

    public SessionService(
            ReservationRepository r,
            ChargingSessionRepository s,
            MeterReadingRepository m,
            ReservationLock l,
            AmqpTemplate e) {
        this(r, s, m, l, e, Clock.systemUTC());
    }

    public SessionService(
            ReservationRepository r,
            ChargingSessionRepository s,
            MeterReadingRepository m,
            ReservationLock l,
            AmqpTemplate e,
            Clock c) {
        reservations = r;
        sessions = s;
        readings = m;
        lock = l;
        events = e;
        clock = c;
    }

    public Reservation reserve(String owner, String station, String connector) {
        if (owner == null || owner.isBlank())
            throw new IllegalArgumentException("owner is required");
        try (AutoCloseable ignored = lock.acquire(connector, Duration.ofSeconds(2))) {
            if (reservations.existsByConnectorIdAndActiveTrue(connector))
                throw new ConflictException("connector already reserved");
            Instant now = Instant.now(clock);
            return reservations.save(
                    new Reservation(
                            owner, station, connector, now, now.plus(Duration.ofMinutes(10))));
        } catch (ConflictException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Reservation reservation(UUID id, String owner) {
        Reservation r =
                reservations.findByIdAndOwnerId(id, owner).orElseThrow(NotFoundException::new);
        expireIfNeeded(r);
        return r;
    }

    public List<Reservation> reservations(String owner) {
        return reservations.findAllByOwnerId(owner).stream().peek(this::expireIfNeeded).toList();
    }

    public Reservation cancel(UUID id, String owner) {
        Reservation r = reservation(id, owner);
        if (r.getStatus() == Reservation.Status.ACTIVE) {
            r.cancel();
            reservations.save(r);
        }
        return r;
    }

    public ChargingSession start(UUID reservationId, String owner) {
        Reservation r = reservation(reservationId, owner);
        if (r.getStatus() != Reservation.Status.ACTIVE)
            throw new ConflictException("reservation cannot start");
        r.started();
        reservations.save(r);
        return sessions.save(new ChargingSession(r, Instant.now(clock)));
    }

    public ChargingSession session(UUID id, String owner) {
        return sessions.findByIdAndOwnerId(id, owner).orElseThrow(NotFoundException::new);
    }

    public List<ChargingSession> sessions(String owner) {
        return sessions.findAllByOwnerId(owner);
    }

    public ChargingSession meter(UUID id, String owner, BigDecimal kwh) {
        if (kwh == null || kwh.signum() < 0)
            throw new IllegalArgumentException("kwh must be non-negative");
        ChargingSession s = session(id, owner);
        if (s.getStatus() != ChargingSession.Status.ACTIVE)
            throw new ConflictException("session is complete");
        s.addEnergy(kwh);
        readings.save(new MeterReading(id, kwh, Instant.now(clock)));
        return sessions.save(s);
    }

    public ChargingSession complete(UUID id, String owner) {
        ChargingSession s = session(id, owner);
        boolean first = s.getStatus() == ChargingSession.Status.ACTIVE;
        s.complete(Instant.now(clock));
        ChargingSession result = sessions.save(s);
        if (first) publish("session.completed", result);
        return result;
    }

    private void expireIfNeeded(Reservation r) {
        if (r.getStatus() == Reservation.Status.ACTIVE
                && !r.getExpiresAt().isAfter(Instant.now(clock))) {
            r.expire();
            reservations.save(r);
        }
    }

    private void publish(String type, ChargingSession s) {
        events.convertAndSend(
                new EventEnvelope(
                        UUID.randomUUID(),
                        type,
                        1,
                        UUID.randomUUID(),
                        Instant.now(clock),
                        Map.of(
                                "sessionId",
                                s.getId(),
                                "ownerId",
                                s.getOwnerId(),
                                "energyKwh",
                                s.getEnergyKwh())));
    }

    public record EventEnvelope(
            UUID eventId,
            String eventType,
            int eventVersion,
            UUID correlationId,
            Instant occurredAt,
            Object payload) {}

    public static class NotFoundException extends RuntimeException {}

    public static class ConflictException extends RuntimeException {
        public ConflictException(String m) {
            super(m);
        }
    }
}
