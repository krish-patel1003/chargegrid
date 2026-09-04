package com.chargegrid.station.service;

import com.chargegrid.station.domain.*;
import com.chargegrid.station.dto.Dtos;
import com.chargegrid.station.web.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class StationService {
    private final Map<String, Station> stations = new HashMap<>();
    private final Map<String, Reservation> reservations = new HashMap<>();
    private final Map<String, ChargingSession> sessions = new HashMap<>();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AtomicLong ids = new AtomicLong(1000);

    public StationService() {
        stations.put("station-1", new Station("station-1", "Central Plaza", 40.7128, -74.0060,
                List.of(new Connector("connector-1", "CCS", 0.45), new Connector("connector-2", "Type2", 0.35))));
        stations.put("station-2", new Station("station-2", "Riverside", 40.7200, -74.0000,
                List.of(new Connector("connector-3", "CCS", 0.50))));
    }
    public synchronized List<Dtos.StationView> nearby(double lat, double lon, double radiusKm) {
        return stations.values().stream().filter(s -> distanceKm(lat, lon, s.latitude(), s.longitude()) <= radiusKm).map(this::stationView).toList();
    }
    public synchronized Station station(String id) { return requireStation(id); }
    public synchronized List<Connector> connectors(String id) { return requireStation(id).connectors(); }
    public synchronized Dtos.ReservationView reserve(String connectorId, String owner) {
        Connector connector = connector(connectorId);
        reservations.values().stream().filter(r -> r.connectorId.equals(connector.id()) && r.status == Reservation.Status.RESERVED && r.expiresAt.isAfter(Instant.now()))
                .findAny().ifPresent(r -> { throw new ApiException(HttpStatus.CONFLICT, "connector is already reserved"); });
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        Reservation r = new Reservation("res-" + ids.incrementAndGet(), connectorId, owner, Instant.now().plusSeconds(600), encoder.encode(code), code);
        reservations.put(r.id, r); return reservationView(r);
    }
    public synchronized Dtos.ReservationView reservation(String id, String owner) { Reservation r = reservation(id); owner(r.ownerId, owner); if (r.status == Reservation.Status.RESERVED && Instant.now().isAfter(r.expiresAt)) r.status = Reservation.Status.EXPIRED; return reservationView(r); }
    public synchronized Dtos.SessionView verifyStart(String id, String owner, String code) {
        Reservation r = reservation(id); owner(r.ownerId, owner);
        if (r.status != Reservation.Status.RESERVED) throw new ApiException(HttpStatus.CONFLICT, "reservation cannot be started");
        if (Instant.now().isAfter(r.expiresAt)) { r.status = Reservation.Status.EXPIRED; throw new ApiException(HttpStatus.GONE, "reservation has expired"); }
        if (r.failedAttempts >= 5) throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too many failed attempts");
        if (!encoder.matches(code, r.startCodeHash)) { r.failedAttempts++; throw new ApiException(HttpStatus.BAD_REQUEST, "invalid start code"); }
        String stop = String.format("%06d", new Random().nextInt(1_000_000));
        ChargingSession s = new ChargingSession("session-" + ids.incrementAndGet(), r.id, r.connectorId, owner, stop, encoder.encode(stop), Instant.now());
        sessions.put(s.id, s); r.status = Reservation.Status.STARTED; r.sessionId = s.id; return sessionView(s);
    }
    public synchronized Dtos.SessionView session(String id, String owner) { ChargingSession s = session(id); owner(s.ownerId, owner); return sessionView(s); }
    public synchronized Dtos.SessionView verifyStop(String id, String owner, String code) {
        ChargingSession s = session(id); owner(s.ownerId, owner);
        if (s.stopped) throw new ApiException(HttpStatus.CONFLICT, "session is already stopped");
        if (!encoder.matches(code, s.stopCodeHash)) throw new ApiException(HttpStatus.BAD_REQUEST, "invalid stop code");
        s.stopped = true; reservations.get(s.reservationId).status = Reservation.Status.COMPLETED; return sessionView(s);
    }
    public synchronized Dtos.SessionView meter(String id, String owner, double kwh) { ChargingSession s = session(id); owner(s.ownerId, owner); if (s.stopped) throw new ApiException(HttpStatus.CONFLICT, "session is stopped"); s.meterKwh += kwh; return sessionView(s); }
    public synchronized Dtos.SessionView meter(String id, double kwh) { ChargingSession s = session(id); if (s.stopped) throw new ApiException(HttpStatus.CONFLICT, "session is stopped"); s.meterKwh += kwh; return sessionView(s); }
    public synchronized Dtos.SimulatorView simulator(String stationId) {
        Station st = requireStation(stationId);
        var rs = reservations.values().stream().filter(r -> st.connectors().stream().anyMatch(c -> c.id().equals(r.connectorId))).map(r -> new Dtos.SimulatorReservation(r.id,r.connectorId,r.ownerId,r.status.name(),r.startCode)).toList();
        var ss = sessions.values().stream().filter(s -> st.connectors().stream().anyMatch(c -> c.id().equals(s.connectorId))).map(s -> new Dtos.SimulatorSession(s.id,s.connectorId,s.ownerId,s.stopCode,s.meterKwh,s.cost(connector(s.connectorId).ratePerKwh()))).toList();
        return new Dtos.SimulatorView(st.id(), st.connectors().stream().map(c -> new Dtos.SimulatorConnector(c.id(),c.type(),c.ratePerKwh())).toList(), rs, ss);
    }
    private Station requireStation(String id) { Station s = stations.get(id); if (s == null) throw new ApiException(HttpStatus.NOT_FOUND, "station not found"); return s; }
    private Connector connector(String id) { return stations.values().stream().flatMap(s -> s.connectors().stream()).filter(c -> c.id().equals(id)).findFirst().orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "connector not found")); }
    private Reservation reservation(String id) { Reservation r = reservations.get(id); if (r == null) throw new ApiException(HttpStatus.NOT_FOUND, "reservation not found"); return r; }
    private ChargingSession session(String id) { ChargingSession s = sessions.get(id); if (s == null) throw new ApiException(HttpStatus.NOT_FOUND, "session not found"); return s; }
    private void owner(String actual, String requested) { if (!actual.equals(requested)) throw new ApiException(HttpStatus.FORBIDDEN, "resource belongs to another user"); }
    private Dtos.StationView stationView(Station s) { return new Dtos.StationView(s.id(),s.name(),s.latitude(),s.longitude(),s.connectors().stream().map(c -> new Dtos.ConnectorView(c.id(),c.type(),c.ratePerKwh())).toList()); }
    private Dtos.ReservationView reservationView(Reservation r) { return new Dtos.ReservationView(r.id,r.connectorId,r.ownerId,r.status.name(),r.expiresAt,r.sessionId); }
    private Dtos.SessionView sessionView(ChargingSession s) { return new Dtos.SessionView(s.id,s.reservationId,s.connectorId,s.ownerId,s.stopped ? "STOPPED" : "CHARGING",s.startedAt,s.meterKwh,s.cost(connector(s.connectorId).ratePerKwh())); }
    private double distanceKm(double a,double b,double c,double d) { double x=Math.toRadians(c-a), y=Math.toRadians(d-b); double q=Math.sin(x/2)*Math.sin(x/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.sin(y/2)*Math.sin(y/2); return 6371*2*Math.atan2(Math.sqrt(q),Math.sqrt(1-q)); }
}
