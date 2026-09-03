package com.chargegrid.user_service.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "service", "user-service",
                "status", "running"
        );
    }
}