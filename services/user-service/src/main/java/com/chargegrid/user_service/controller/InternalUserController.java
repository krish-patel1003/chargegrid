package com.chargegrid.user_service.controller;

import com.chargegrid.user_service.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service-to-service lookups.
 *
 * <p>Mounted under {@code /internal} rather than {@code /api} because the gateway only routes
 * {@code /api/**}: nothing here is reachable from outside the cluster. notification-service uses it
 * to turn the subject on an event into an address to email, so events never have to carry PII.
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserProfileRepository profiles;

    public InternalUserController(UserProfileRepository profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/{keycloakUserId}")
    public Recipient byKeycloakId(@PathVariable String keycloakUserId) {
        return profiles.findByKeycloakUserId(keycloakUserId)
                .map(p -> new Recipient(p.getKeycloakUserId(), p.getEmail(), p.getDisplayName()))
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown user"));
    }

    public record Recipient(String keycloakUserId, String email, String displayName) {}
}
