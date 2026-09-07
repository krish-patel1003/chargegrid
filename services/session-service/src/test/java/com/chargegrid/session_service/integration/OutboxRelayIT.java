package com.chargegrid.session_service.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.chargegrid.session_service.catalog.StationCatalog;
import com.chargegrid.session_service.domain.Reservation;
import com.chargegrid.session_service.dto.Dtos;
import com.chargegrid.session_service.outbox.OutboxEvent;
import com.chargegrid.session_service.outbox.OutboxRelay;
import com.chargegrid.session_service.outbox.OutboxRepository;
import com.chargegrid.session_service.repository.ChargingSessionRepository;
import com.chargegrid.session_service.repository.MeterReadingRepository;
import com.chargegrid.session_service.repository.ReservationRepository;
import com.chargegrid.session_service.service.SessionService;
import com.chargegrid.session_service.support.RequiresInfrastructure;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

/**
 * The outbox is what makes delivery at-least-once, and that only holds if the event lands in the
 * same transaction as the session and the relay then drains it. Both halves need a real database
 * and a real broker.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("integration")
class OutboxRelayIT {

    private static final String STATION = "it-outbox-station";

    @Autowired private SessionService sessions;
    @Autowired private OutboxRepository outbox;
    @Autowired private OutboxRelay relay;
    @Autowired private ReservationRepository reservations;
    @Autowired private ChargingSessionRepository chargingSessions;
    @Autowired private MeterReadingRepository readings;

    @TestConfiguration
    static class StubCatalog {
        @Bean
        @Primary
        StationCatalog stationCatalog() {
            return connectorId ->
                    new StationCatalog.ConnectorDetail(
                            connectorId, STATION, "CCS", 150, true, new BigDecimal("0.500000"));
        }
    }

    @BeforeAll
    static void requireInfrastructure() {
        RequiresInfrastructure.check();
    }

    @AfterEach
    void cleanUp() {
        // Meter readings reference the session, so they go first.
        chargingSessions
                .findAllByStationId(STATION)
                .forEach(
                        session ->
                                readings.deleteAll(
                                        readings.findAllBySessionIdOrderByRecordedAtAsc(
                                                session.getId())));
        chargingSessions.deleteAll(chargingSessions.findAllByStationId(STATION));
        reservations.deleteAll(reservations.findAllByStationId(STATION));
    }

    @Test
    void completingASessionWritesAnEventTheRelayThenPublishes() {
        String connectorId = "connector-" + UUID.randomUUID();
        String owner = "driver-" + UUID.randomUUID();

        Dtos.ReservationView reservation = sessions.reserve(owner, connectorId);
        Reservation stored = reservations.findById(UUID.fromString(reservation.id())).orElseThrow();
        Dtos.SessionView session =
                sessions.verifyStart(stored.getId(), owner, stored.getStartCodeDisplay());
        sessions.meter(UUID.fromString(session.id()), new BigDecimal("4"));

        var active = chargingSessions.findById(UUID.fromString(session.id())).orElseThrow();
        sessions.verifyStop(active.getId(), owner, active.getStopCodeDisplay());

        // Written in the completing transaction, not yet on the broker.
        OutboxEvent event = eventFor(session.id());
        assertThat(event.getEventType()).isEqualTo("ChargingSessionCompleted");
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getPayload())
                .contains("\"eventId\":\"" + event.getId() + "\"")
                .contains("\"ownerId\":\"" + owner + "\"")
                // 4 kWh at the 0.50 rate captured when the driver reserved.
                .contains("\"cost\":2.00");

        relay.relay();

        assertThat(eventFor(session.id()).getPublishedAt()).isNotNull();
        assertThat(eventFor(session.id()).getLastError()).isNull();
    }

    private OutboxEvent eventFor(String sessionId) {
        List<OutboxEvent> all = outbox.findAll();
        return all.stream()
                .filter(e -> e.getAggregateId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no outbox event for session " + sessionId));
    }
}
