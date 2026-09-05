package com.chargegrid.station.web;

import com.chargegrid.station.dto.Dtos;
import com.chargegrid.station.service.StationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final StationService service;
    private final String adminKey;

    public AdminController(
            StationService service, @Value("${station.admin-key:demo-admin-key}") String adminKey) {
        this.service = service;
        this.adminKey = adminKey;
    }

    @GetMapping("/stations/{stationId}/simulator")
    public Dtos.SimulatorView simulator(
            @PathVariable String stationId,
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        check(key);
        return service.simulator(stationId);
    }

    @PostMapping("/sessions/{sessionId}/meter")
    public Dtos.SessionView meter(
            @PathVariable String sessionId,
            @Valid @RequestBody Dtos.MeterRequest request,
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        check(key);
        return service.meter(sessionId, request.kwh());
    }

    private void check(String key) {
        if (!adminKey.equals(key))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "admin key required");
    }
}
