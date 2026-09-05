package com.chargegrid.user_service.service;

import com.chargegrid.user_service.dto.CurrentUserClaims;
import com.chargegrid.user_service.dto.UserProfileResponse;
import com.chargegrid.user_service.entity.UserProfile;
import com.chargegrid.user_service.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {
    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserProfileResponse getOrProvision(CurrentUserClaims claims) {
        UserProfile profile =
                repository
                        .findByKeycloakUserId(claims.subject())
                        .orElseGet(
                                () ->
                                        new UserProfile(
                                                claims.subject(),
                                                claims.email(),
                                                claims.displayName()));

        profile.updateFromClaims(claims.email(), claims.displayName());
        return toResponse(repository.save(profile));
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getKeycloakUserId(),
                profile.getEmail(),
                profile.getDisplayName(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
