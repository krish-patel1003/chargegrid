package com.chargegrid.user_service.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String keycloakUserId,
        String email,
        String displayName,
        Instant createdAt,
        Instant updatedAt) {}
