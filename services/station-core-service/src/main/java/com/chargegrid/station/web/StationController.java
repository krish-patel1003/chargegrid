package com.chargegrid.station.web;

import com.chargegrid.station.dto.Dtos;
import com.chargegrid.station.service.StationService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stations")
public class StationController {
    private final StationService service;

    public StationController(StationService service) {
        this.service = service;
    }

    @GetMapping("/nearby")
    public List<Dtos.StationView> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "10") double radiusKm) {
        return service.nearby(latitude, longitude, radiusKm);
    }

    @GetMapping("/{stationId}")
    public Dtos.StationView station(@PathVariable String stationId) {
        var s = service.station(stationId);
        return new Dtos.StationView(
                s.id(),
                s.name(),
                s.latitude(),
                s.longitude(),
                s.connectors().stream()
                        .map(c -> new Dtos.ConnectorView(c.id(), c.type(), c.ratePerKwh()))
                        .toList());
    }

    @GetMapping("/{stationId}/connectors")
    public List<Dtos.ConnectorView> connectors(@PathVariable String stationId) {
        return service.connectors(stationId).stream()
                .map(c -> new Dtos.ConnectorView(c.id(), c.type(), c.ratePerKwh()))
                .toList();
    }
}
