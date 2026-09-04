package com.chargegrid.discovery.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "stations")
public class Station {
    @Id
    private UUID id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;

    protected Station() { }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
