package com.chargegrid.discovery.web;

import com.chargegrid.discovery.service.StationService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/stations")
public class StationController {
    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/nearby")
    public List<StationService.StationResponse> nearby(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @RequestParam(defaultValue = "25") @Positive double radiusKm,
            @RequestParam(required = false) String connectorType,
            @RequestParam(required = false) Boolean available) {
        return stationService.nearby(latitude, longitude, radiusKm, connectorType, available);
    }

    @GetMapping("/{id}")
    public StationService.StationResponse get(@PathVariable UUID id) {
        return stationService.get(id);
    }

    @GetMapping("/{id}/connectors")
    public List<StationService.ConnectorResponse> connectors(@PathVariable UUID id) {
        return stationService.connectors(id);
    }
}
