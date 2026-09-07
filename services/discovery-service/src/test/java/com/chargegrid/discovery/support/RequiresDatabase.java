package com.chargegrid.discovery.support;

import java.sql.DriverManager;
import org.junit.jupiter.api.Assumptions;

/**
 * Integration tests need a real PostGIS instance.
 *
 * <p>Locally they skip when one is not running, so {@code mvn test} still works on a clean
 * checkout. In CI {@code CHARGEGRID_REQUIRE_INTEGRATION=true} turns that skip into a failure, so a
 * missing database can never quietly reduce the suite to nothing.
 */
public final class RequiresDatabase {

    private RequiresDatabase() {}

    public static void check() {
        String url =
                env(
                        "INTEGRATION_DATABASE_URL",
                        "jdbc:postgresql://localhost:5432/chargegrid_discovery");
        String user = env("INTEGRATION_DATABASE_USERNAME", "chargegrid");
        String password = env("INTEGRATION_DATABASE_PASSWORD", "chargegrid_dev");
        try (var ignored = DriverManager.getConnection(url, user, password)) {
            // Reachable.
        } catch (Exception e) {
            if (Boolean.parseBoolean(System.getenv("CHARGEGRID_REQUIRE_INTEGRATION"))) {
                throw new IllegalStateException(
                        "CHARGEGRID_REQUIRE_INTEGRATION is set but " + url + " is unreachable", e);
            }
            Assumptions.abort("No PostGIS at " + url + " - skipping integration test");
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
