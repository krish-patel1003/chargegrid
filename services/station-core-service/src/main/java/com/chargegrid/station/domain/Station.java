package com.chargegrid.station.domain;

import java.util.List;

public record Station(
        String id, String name, double latitude, double longitude, List<Connector> connectors) {}
