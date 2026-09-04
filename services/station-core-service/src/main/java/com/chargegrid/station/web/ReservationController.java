package com.chargegrid.station.web;

import com.chargegrid.station.dto.Dtos;
import com.chargegrid.station.service.StationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final StationService service;
    public ReservationController(StationService service) { this.service = service; }
    @PostMapping public Dtos.ReservationView reserve(@Valid @RequestBody Dtos.ReservationRequest request,
                                                      @RequestHeader(value="X-User-Id", defaultValue="demo-driver") String owner) { return service.reserve(request.connectorId(), owner); }
    @GetMapping("/{id}") public Dtos.ReservationView get(@PathVariable String id, @RequestHeader(value="X-User-Id", defaultValue="demo-driver") String owner) { return service.reservation(id, owner); }
    @PostMapping("/{id}/verify-start") public Dtos.SessionView start(@PathVariable String id, @Valid @RequestBody Dtos.CodeRequest request,
                                                                         @RequestHeader(value="X-User-Id", defaultValue="demo-driver") String owner) { return service.verifyStart(id, owner, request.code()); }
}
