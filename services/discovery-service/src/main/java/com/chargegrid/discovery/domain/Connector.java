package com.chargegrid.discovery.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "connectors")
public class Connector {
    @Id
    private UUID id;
    private String connectorType;
    private int powerKw;
    private boolean available;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    protected Connector() { }

    public UUID getId() { return id; }
    public String getConnectorType() { return connectorType; }
    public int getPowerKw() { return powerKw; }
    public boolean isAvailable() { return available; }
}
