package com.chargegrid.user_service.repository;

import com.chargegrid.user_service.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository
        extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByKeycloakUserId(String keycloakUserId);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByKeycloakUserId(String keycloakUserId);

    boolean existsByEmail(String email);
}
