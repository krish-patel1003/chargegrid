package com.chargegrid.session_service.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.chargegrid.session_service.catalog.StationCatalog;
import com.chargegrid.session_service.repository.ReservationRepository;
import com.chargegrid.session_service.service.SessionService;
import com.chargegrid.session_service.support.RequiresInfrastructure;
import com.chargegrid.session_service.web.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
 * The invariant the whole reservation flow rests on: two drivers must never hold the same
 * connector.
 *
 * <p>This cannot be shown with mocks. It needs the real Redis lock and the real partial unique
 * index {@code uk_active_connector}, with genuine threads racing.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("integration")
class ReservationConcurrencyIT {

    private static final int RACERS = 12;
    private static final String STATION = "it-station";

    @Autowired private SessionService sessions;
    @Autowired private ReservationRepository reservations;

    /** Avoids needing discovery-service running; the race is about locking, not the catalogue. */
    @TestConfiguration
    static class StubCatalog {
        @Bean
        @Primary
        StationCatalog stationCatalog() {
            return connectorId ->
                    new StationCatalog.ConnectorDetail(
                            connectorId, STATION, "CCS", 150, true, new BigDecimal("0.400000"));
        }
    }

    private String connectorId;

    @BeforeAll
    static void requireInfrastructure() {
        RequiresInfrastructure.check();
    }

    @AfterEach
    void cleanUp() {
        if (connectorId != null) {
            reservations.deleteAll(reservations.findAllByStationId(STATION));
        }
    }

    @Test
    void exactlyOneDriverWinsAConnector() throws Exception {
        connectorId = "connector-" + UUID.randomUUID();
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger won = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        List<Callable<Void>> racers =
                java.util.stream.IntStream.range(0, RACERS)
                        .<Callable<Void>>mapToObj(
                                i ->
                                        () -> {
                                            startLine.await();
                                            try {
                                                sessions.reserve("driver-" + i, connectorId);
                                                won.incrementAndGet();
                                            } catch (ApiException e) {
                                                // "already reserved" from the index, or "busy" from
                                                // the lock: both are correct refusals.
                                                rejected.incrementAndGet();
                                            }
                                            return null;
                                        })
                        .toList();

        ExecutorService pool = Executors.newFixedThreadPool(RACERS);
        try {
            List<Future<Void>> futures = racers.stream().map(pool::submit).toList();
            startLine.countDown();
            for (Future<Void> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(won.get()).as("exactly one driver holds the connector").isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(RACERS - 1);
        assertThat(reservations.findAllByStationId(STATION))
                .filteredOn(r -> r.getConnectorId().equals(connectorId))
                .filteredOn(r -> r.isActive())
                .hasSize(1);
    }

    @Test
    void aConnectorIsReusableOnceTheHoldIsReleased() {
        connectorId = "connector-" + UUID.randomUUID();

        var first = sessions.reserve("driver-a", connectorId);
        sessions.cancel(UUID.fromString(first.id()), "driver-a");

        // The partial index only constrains active rows, so cancelling frees it.
        var second = sessions.reserve("driver-b", connectorId);

        assertThat(second.ownerId()).isEqualTo("driver-b");
    }
}
