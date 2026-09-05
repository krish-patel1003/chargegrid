# ChargeGrid

ChargeGrid is a demo EV charging journey: authenticate, discover a nearby
station, reserve a connector, verify charger codes, run a charging session, and
settle it through Stripe.

## Services

| Service | Port | Responsibility |
| --- | --- | --- |
| `api-gateway` | 8088 | Routing, CORS, and JWT validation against Keycloak |
| `user-service` | 8081 | User profiles keyed by Keycloak subject |
| `discovery-service` | 8082 | Station catalogue, PostGIS proximity search, connector tariffs |
| `session-service` | 8083 | Reservations, charger-code handshake, metering, session events |
| `billing-service` | 8085 | Stripe invoices, idempotent settlement |
| `notification-service` | 8086 | Email delivery driven off session events |

Each service owns its own database and its own Flyway history. They must not
share one: two services migrating into the same database collide on schema
history version 1. `infrastructure/postgres/init/` creates one database per
service on first start.

## Run locally

1. Copy `.env.example` to `.env` and replace the local values.
2. Start the infrastructure: `docker compose up -d`
   (PostgreSQL/PostGIS, Redis, RabbitMQ, Keycloak).
3. Start the services: `docker compose --profile apps up -d --build`.
4. Start the frontend: `cd frontend && npm install && npm run dev`.
5. Open `http://localhost:5173/login`.

Keycloak is at `http://localhost:8080`, RabbitMQ management at
`http://localhost:15672`, and PostgreSQL/PostGIS on port `5432`.

Without step 3 the frontend still runs and falls back to demo stations.

### Walking the charging flow

Reserving a connector returns a six-digit start code that a real charger would
display. The operator view stands in for that hardware:

```
http://localhost:5173/admin/stations/<stationId>/simulator
```

It shows the current start and stop codes and lets you push meter readings.
It is guarded by `STATION_ADMIN_KEY`, because those endpoints belong to the
charging hardware rather than to a driver.

## Development

- Java is formatted by Spotless (google-java-format, AOSP). `spotless:check` is
  bound to the `validate` phase, so `mvn test` fails on unformatted sources.
  Run `mvn spotless:apply` to fix.
- The frontend is formatted by Prettier: `npm run format`, checked in CI with
  `npm run format:check`.
- CI builds every service, builds the frontend, and validates the k8s manifests.

## Known gaps

- Login is a demo button. The frontend sends a fixed `X-User-Id` rather than a
  Keycloak token subject, and the gateway does not yet forward the JWT subject
  downstream.
- The payment screen is a mock; card capture is not wired to Stripe Elements.
- There are no integration tests yet against a real PostGIS or Redis.

## Security

Real secrets belong only in local environment files or deployment secret
stores. Never commit Stripe, Resend, Keycloak, JWT, or database credentials.
