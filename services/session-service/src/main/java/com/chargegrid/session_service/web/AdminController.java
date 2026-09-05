package com.chargegrid.session_service.web;

import com.chargegrid.session_service.dto.Dtos;
import com.chargegrid.session_service.service.SessionService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints the charging hardware calls. The simulator front-end stands in for that hardware, which
 * is why it needs the shared operator key rather than a driver's token.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SessionService service;
    private final String adminKey;

    public AdminController(
            SessionService service, @Value("${chargegrid.admin-key}") String adminKey) {
        this.service = service;
        this.adminKey = adminKey;
    }

    @GetMapping("/stations/{stationId}/simulator")
    public Dtos.SimulatorView simulator(
            @PathVariable String stationId,
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireOperator(key);
        return service.simulator(stationId);
    }

    @PostMapping("/sessions/{sessionId}/meter")
    public Dtos.SessionView meter(
            @PathVariable UUID sessionId,
            @Valid @RequestBody Dtos.MeterRequest request,
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireOperator(key);
        return service.meter(sessionId, request.kwh());
    }

    private void requireOperator(String key) {
        if (key == null
                || !MessageDigest.isEqual(
                        key.getBytes(StandardCharsets.UTF_8),
                        adminKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "operator key required");
        }
    }
}
