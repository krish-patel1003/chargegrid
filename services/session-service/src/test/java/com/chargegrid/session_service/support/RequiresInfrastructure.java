package com.chargegrid.session_service.support;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.DriverManager;
import org.junit.jupiter.api.Assumptions;

/**
 * Integration tests need real PostgreSQL, Redis and RabbitMQ.
 *
 * <p>Locally they skip when those are not running, so {@code mvn test} still works on a clean
 * checkout. In CI {@code CHARGEGRID_REQUIRE_INTEGRATION=true} turns the skip into a failure, so
 * missing infrastructure cannot quietly reduce the suite to nothing.
 */
public final class RequiresInfrastructure {

    private RequiresInfrastructure() {}

    public static void check() {
        database();
        port(
                env("INTEGRATION_REDIS_HOST", "localhost"),
                intEnv("INTEGRATION_REDIS_PORT", 6379),
                "Redis");
        port(
                env("INTEGRATION_RABBITMQ_HOST", "localhost"),
                intEnv("INTEGRATION_RABBITMQ_PORT", 5672),
                "RabbitMQ");
    }

    private static void database() {
        String url =
                env(
                        "INTEGRATION_DATABASE_URL",
                        "jdbc:postgresql://localhost:5432/chargegrid_sessions");
        try (var ignored =
                DriverManager.getConnection(
                        url,
                        env("INTEGRATION_DATABASE_USERNAME", "chargegrid"),
                        env("INTEGRATION_DATABASE_PASSWORD", "chargegrid_dev"))) {
            // Reachable.
        } catch (Exception e) {
            unavailable("PostgreSQL at " + url, e);
        }
    }

    private static void port(String host, int port, String what) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1500);
        } catch (Exception e) {
            unavailable(what + " at " + host + ":" + port, e);
        }
    }

    private static void unavailable(String what, Exception cause) {
        if (Boolean.parseBoolean(System.getenv("CHARGEGRID_REQUIRE_INTEGRATION"))) {
            throw new IllegalStateException(
                    "CHARGEGRID_REQUIRE_INTEGRATION is set but " + what + " is unreachable", cause);
        }
        Assumptions.abort("No " + what + " - skipping integration test");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int intEnv(String name, int fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
    }
}
