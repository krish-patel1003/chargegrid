package com.chargegrid.user_service.controller;

import com.chargegrid.user_service.dto.CurrentUserClaims;
import com.chargegrid.user_service.dto.UserProfileResponse;
import com.chargegrid.user_service.service.UserProfileService;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "service", "user-service",
                "status", "running");
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.getOrProvision(CurrentUserClaims.from(jwt));
    }
}
