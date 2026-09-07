package com.chargegrid.notification_service.directory;

import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Turns the subject on an event into an address to email.
 *
 * <p>Events carry {@code ownerId}, never an address: an email is personal data that changes, and
 * baking it into an immutable event means every replay reuses a stale one. user-service owns the
 * profile, so it is asked at delivery time.
 */
@Component
public class RecipientDirectory {

    private final RestClient client;

    public RecipientDirectory(
            @Value("${chargegrid.user-service.url:http://localhost:8081}") String baseUrl,
            @Value("${chargegrid.user-service.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${chargegrid.user-service.read-timeout-ms:3000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.client = RestClient.builder().requestFactory(factory).baseUrl(baseUrl).build();
    }

    /**
     * @return the profile, or empty when the user is unknown. A transport failure throws, so the
     *     message is retried rather than silently dropped.
     */
    public Optional<Recipient> lookup(String keycloakUserId) {
        try {
            return Optional.ofNullable(
                    client.get()
                            .uri("/internal/users/{id}", keycloakUserId)
                            .retrieve()
                            .onStatus(
                                    status -> status.value() == 404,
                                    (request, response) -> {
                                        throw new UnknownRecipientException(keycloakUserId);
                                    })
                            .body(Recipient.class));
        } catch (UnknownRecipientException e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new DirectoryUnavailableException(keycloakUserId, e);
        }
    }

    public record Recipient(String keycloakUserId, String email, String displayName) {}

    static class UnknownRecipientException extends RuntimeException {
        UnknownRecipientException(String id) {
            super("No profile for " + id);
        }
    }

    public static class DirectoryUnavailableException extends RuntimeException {
        public DirectoryUnavailableException(String id, Throwable cause) {
            super("Could not resolve recipient for " + id, cause);
        }
    }
}
