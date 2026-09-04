# Station Core Service

Spring Boot 4.1.1 / Java 21 in-memory station, reservation, and charging-session demo.

Run with `mvn test` and start with `mvn spring-boot:run`. Health is available at `GET /actuator/health`.

## API

- `GET /api/stations/nearby?latitude=40.7128&longitude=-74.006&radiusKm=10`
- `GET /api/stations/{stationId}` and `GET /api/stations/{stationId}/connectors`
- `POST /api/reservations` with `{ "connectorId": "connector-1" }`
- `GET /api/reservations/{id}` and `POST /api/reservations/{id}/verify-start` with `{ "code": "123456" }`
- `GET /api/sessions/{id}` and `POST /api/sessions/{id}/verify-stop` with `{ "code": "123456" }`

`X-User-Id` identifies the driver and defaults to `demo-driver`. Reservations expire after ten minutes and use synchronized connector conflict checks. Five failed start-code attempts return 429; invalid, expired, reused, and unauthorized operations return 400, 410, 409, and 403 as appropriate.

The protected simulator endpoint is `GET /api/admin/stations/{stationId}/simulator` with `X-Admin-Key: demo-admin-key` (or `STATION_ADMIN_KEY`). It is the only endpoint that returns plaintext start/stop codes. `POST /api/admin/sessions/{sessionId}/meter` with `{ "kwh": 1.5 }` increments the demo meter; costs are calculated using the connector's per-kWh rate.
