package com.chargegrid.session_service.web;

import org.springframework.http.HttpStatus;

/**
 * Resolves the calling driver.
 *
 * <p>The gateway validates the Keycloak access token and is expected to forward the subject as
 * {@code X-User-Id}. Requests without it are rejected rather than defaulted, so a missing header
 * can never silently pool unrelated drivers into one shared identity.
 */
final class CallerId {

    private CallerId() {}

    static String require(String header) {
        if (header == null || header.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "X-User-Id header is required");
        }
        return header;
    }
}
