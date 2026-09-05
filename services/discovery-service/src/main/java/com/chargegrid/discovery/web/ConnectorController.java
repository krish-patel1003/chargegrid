package com.chargegrid.discovery.web;

import com.chargegrid.discovery.service.StationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/connectors")
public class ConnectorController {
    private final StationService stationService;

    public ConnectorController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/{id}")
    public StationService.ConnectorDetail get(@PathVariable UUID id) {
        return stationService.connector(id);
    }
}
