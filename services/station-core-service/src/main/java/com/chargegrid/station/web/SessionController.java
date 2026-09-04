package com.chargegrid.station.web;

import com.chargegrid.station.dto.Dtos;
import com.chargegrid.station.service.StationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private final StationService service;
    public SessionController(StationService service) { this.service = service; }
    @GetMapping("/{id}") public Dtos.SessionView get(@PathVariable String id, @RequestHeader(value="X-User-Id", defaultValue="demo-driver") String owner) { return service.session(id, owner); }
    @PostMapping("/{id}/verify-stop") public Dtos.SessionView stop(@PathVariable String id, @Valid @RequestBody Dtos.CodeRequest request,
                                                                       @RequestHeader(value="X-User-Id", defaultValue="demo-driver") String owner) { return service.verifyStop(id, owner, request.code()); }
}
