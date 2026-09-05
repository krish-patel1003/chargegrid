package com.chargegrid.session_service.web;

import com.chargegrid.session_service.dto.Dtos;
import com.chargegrid.session_service.service.SessionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final SessionService service;

    public ReservationController(SessionService service) {
        this.service = service;
    }

    @PostMapping
    public Dtos.ReservationView reserve(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody Dtos.ReserveRequest request) {
        return service.reserve(CallerId.require(userId), request.connectorId());
    }

    @GetMapping
    public List<Dtos.ReservationView> list(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return service.reservations(CallerId.require(userId));
    }

    @GetMapping("/{id}")
    public Dtos.ReservationView get(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id) {
        return service.reservation(id, CallerId.require(userId));
    }

    @PostMapping("/{id}/cancel")
    public Dtos.ReservationView cancel(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id) {
        return service.cancel(id, CallerId.require(userId));
    }

    @PostMapping("/{id}/verify-start")
    public Dtos.SessionView verifyStart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id,
            @Valid @RequestBody Dtos.CodeRequest request) {
        return service.verifyStart(id, CallerId.require(userId), request.code());
    }
}
