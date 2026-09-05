package com.chargegrid.session_service.web;

import com.chargegrid.session_service.domain.*;
import com.chargegrid.session_service.service.SessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SessionController {
    private final SessionService service;

    public SessionController(SessionService s) {
        service = s;
    }

    private String owner(String value) {
        return value == null || value.isBlank() ? "anonymous" : value;
    }

    @PostMapping("/reservations")
    public Reservation reserve(
            @RequestHeader(value = "X-User-Id", required = false) String u,
            @Valid @RequestBody ReserveRequest q) {
        return service.reserve(owner(u), q.stationId(), q.connectorId());
    }

    @GetMapping("/reservations")
    public List<Reservation> reservations(
            @RequestHeader(value = "X-User-Id", required = false) String u) {
        return service.reservations(owner(u));
    }

    @GetMapping("/reservations/{id}")
    public Reservation reservation(
            @RequestHeader(value = "X-User-Id", required = false) String u, @PathVariable UUID id) {
        return service.reservation(id, owner(u));
    }

    @PostMapping("/reservations/{id}/cancel")
    public Reservation cancel(
            @RequestHeader(value = "X-User-Id", required = false) String u, @PathVariable UUID id) {
        return service.cancel(id, owner(u));
    }

    @PostMapping("/reservations/{id}/start")
    public ChargingSession start(
            @RequestHeader(value = "X-User-Id", required = false) String u, @PathVariable UUID id) {
        return service.start(id, owner(u));
    }

    @GetMapping("/sessions/{id}")
    public ChargingSession session(
            @RequestHeader(value = "X-User-Id", required = false) String u, @PathVariable UUID id) {
        return service.session(id, owner(u));
    }

    @GetMapping("/sessions")
    public List<ChargingSession> sessions(
            @RequestHeader(value = "X-User-Id", required = false) String u) {
        return service.sessions(owner(u));
    }

    @PostMapping("/sessions/{id}/meter")
    public ChargingSession meter(
            @RequestHeader(value = "X-User-Id", required = false) String u,
            @PathVariable UUID id,
            @Valid @RequestBody MeterRequest q) {
        return service.meter(id, owner(u), q.kwh());
    }

    @PostMapping("/sessions/{id}/complete")
    public ChargingSession complete(
            @RequestHeader(value = "X-User-Id", required = false) String u, @PathVariable UUID id) {
        return service.complete(id, owner(u));
    }

    public record ReserveRequest(@NotBlank String stationId, @NotBlank String connectorId) {}

    public record MeterRequest(@NotNull @DecimalMin("0.0") BigDecimal kwh) {}

    @ExceptionHandler(SessionService.NotFoundException.class)
    ResponseEntity<Void> missing() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({SessionService.ConflictException.class, IllegalArgumentException.class})
    ResponseEntity<String> conflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
