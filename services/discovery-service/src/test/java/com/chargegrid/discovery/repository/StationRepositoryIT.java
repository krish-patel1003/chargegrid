package com.chargegrid.discovery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.chargegrid.discovery.support.RequiresDatabase;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Exercises the spatial query against real PostGIS.
 *
 * <p>{@code findNearby} is native SQL built on ST_DWithin and ST_Distance, so a unit test with a
 * mocked repository proves nothing about it. Assertions run against the two stations seeded by V1,
 * whose coordinates are fixed, so no fixture setup or cleanup is needed.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("integration")
class StationRepositoryIT {

    // Long Beach Civic Center is at 33.7683 / -118.1956, the Marina ~0.8 km away.
    private static final double CIVIC_CENTER_LAT = 33.7683;
    private static final double CIVIC_CENTER_LON = -118.1956;

    @Autowired private StationRepository stations;

    @BeforeAll
    static void requireDatabase() {
        RequiresDatabase.check();
    }

    @Test
    void findsSeededStationsWithinTheRadius() {
        List<StationRepository.NearbyStation> found =
                stations.findNearby(CIVIC_CENTER_LAT, CIVIC_CENTER_LON, 25, null, null);

        assertThat(found)
                .extracting(StationRepository.NearbyStation::getName)
                .contains("Long Beach Civic Center", "Long Beach Marina");
    }

    @Test
    void ordersByRealDistanceAndComputesItInKilometres() {
        List<StationRepository.NearbyStation> found =
                stations.findNearby(CIVIC_CENTER_LAT, CIVIC_CENTER_LON, 25, null, null);

        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getName()).isEqualTo("Long Beach Civic Center");
        // Standing on it, so essentially zero.
        assertThat(found.get(0).getDistanceKm()).isLessThan(0.1);
        assertThat(found).extracting(StationRepository.NearbyStation::getDistanceKm).isSorted();
    }

    @Test
    void radiusActuallyExcludes() {
        // 0.1 km around the Civic Center keeps it and drops the Marina.
        List<StationRepository.NearbyStation> tight =
                stations.findNearby(CIVIC_CENTER_LAT, CIVIC_CENTER_LON, 0.1, null, null);

        assertThat(tight)
                .extracting(StationRepository.NearbyStation::getName)
                .containsExactly("Long Beach Civic Center");
    }

    @Test
    void farAwayCoordinatesFindNothing() {
        // Reykjavik.
        assertThat(stations.findNearby(64.1466, -21.9426, 25, null, null)).isEmpty();
    }

    @Test
    void filtersByConnectorType() {
        List<StationRepository.NearbyStation> chademo =
                stations.findNearby(CIVIC_CENTER_LAT, CIVIC_CENTER_LON, 25, "CHAdeMO", null);

        // Only the Marina has a CHAdeMO connector.
        assertThat(chademo)
                .extracting(StationRepository.NearbyStation::getName)
                .containsExactly("Long Beach Marina");
    }

    @Test
    void filtersByAvailability() {
        List<StationRepository.NearbyStation> unavailable =
                stations.findNearby(CIVIC_CENTER_LAT, CIVIC_CENTER_LON, 25, null, false);

        // The Marina's CCS connector is seeded unavailable; the Civic Center has none.
        assertThat(unavailable)
                .extracting(StationRepository.NearbyStation::getName)
                .containsExactly("Long Beach Marina");
    }
}
