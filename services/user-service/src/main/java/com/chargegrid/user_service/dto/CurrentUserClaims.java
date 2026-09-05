package com.chargegrid.user_service.dto;

import org.springframework.security.oauth2.jwt.Jwt;

public record CurrentUserClaims(String subject, String email, String displayName) {

    public static CurrentUserClaims from(Jwt jwt) {
        String subject = required(jwt.getSubject(), "sub");
        String email = required(jwt.getClaimAsString("email"), "email");
        String displayName =
                firstNonBlank(
                        jwt.getClaimAsString("name"),
                        jwt.getClaimAsString("preferred_username"),
                        email);
        return new CurrentUserClaims(subject, email, displayName);
    }

    private static String required(String value, String claim) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT claim '%s' is required".formatted(claim));
        }
        return value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new IllegalArgumentException("JWT display name claim is required");
    }
}
