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
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Dtos.SessionView> list(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return service.sessions(CallerId.require(userId));
    }

    @GetMapping("/{id}")
    public Dtos.SessionView get(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id) {
        return service.session(id, CallerId.require(userId));
    }

    @PostMapping("/{id}/verify-stop")
    public Dtos.SessionView verifyStop(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id,
            @Valid @RequestBody Dtos.CodeRequest request) {
        return service.verifyStop(id, CallerId.require(userId), request.code());
    }
}
