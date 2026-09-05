package com.chargegrid.discovery.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "connectors")
public class Connector {
    @Id private UUID id;

    private String connectorType;
    private int powerKw;
    private boolean available;

    @Column(name = "rate_per_kwh", nullable = false)
    private BigDecimal ratePerKwh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    /** Read-only projection of the join column, so callers never touch the lazy proxy. */
    @Column(name = "station_id", insertable = false, updatable = false)
    private UUID stationId;

    protected Connector() {}

    public UUID getId() {
        return id;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public int getPowerKw() {
        return powerKw;
    }

    public boolean isAvailable() {
        return available;
    }

    public BigDecimal getRatePerKwh() {
        return ratePerKwh;
    }

    public UUID getStationId() {
        return stationId;
    }
}
