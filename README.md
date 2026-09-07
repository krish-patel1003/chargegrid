# ChargeGrid

An EV charging platform: find a station, reserve a connector, prove you are
standing at it, draw energy, and settle the bill. Six Spring Boot services
behind an API gateway, a React front end, Keycloak for identity, PostGIS for
spatial search, Redis and RabbitMQ for coordination.

![Discovery screen with map and nearby stations](docs/screenshots/03-discover.png)

## The journey

<table>
<tr>
<td width="50%"><img src="docs/screenshots/01-login.png" alt="Sign-in screen"></td>
<td width="50%"><img src="docs/screenshots/02-keycloak.png" alt="Keycloak hosted login"></td>
</tr>
<tr>
<td>Sign-in, which redirects to Keycloak…</td>
<td>…using Authorization Code flow with PKCE.</td>
</tr>
<tr>
<td><img src="docs/screenshots/04-station.png" alt="Station detail with connectors"></td>
<td><img src="docs/screenshots/05-reservation.png" alt="Reservation with countdown"></td>
</tr>
<tr>
<td>Connectors with live availability and per-kWh price.</td>
<td>A ten-minute hold, counting down.</td>
</tr>
<tr>
<td><img src="docs/screenshots/06-simulator.png" alt="Operator simulator showing start code"></td>
<td><img src="docs/screenshots/09-session-metered.png" alt="Live charging session"></td>
</tr>
<tr>
<td>The operator display stands in for the charger, showing the start code.</td>
<td>Energy metered by the charger, priced at the reserved rate.</td>
</tr>
</table>

![Charging history](docs/screenshots/10-activity.png)

## Run it

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env

docker compose up -d                          # Postgres/PostGIS, Redis, RabbitMQ, Keycloak
docker compose --profile apps up -d --build   # the six services

cd frontend && npm install && npm run dev
```

Open <http://localhost:5173> and sign in as **`driver` / `driver`** (seeded by
the realm import).

To walk the whole flow, open the operator display for a station in a second tab:

```
http://localhost:5173/admin/stations/<stationId>/simulator
```

It shows the codes the charger would display and lets you push meter readings.

Keycloak is at <http://localhost:8080>, RabbitMQ management at
<http://localhost:15672>.

## How it fits together

| Service | Port | Owns |
| --- | --- | --- |
| `api-gateway` | 8088 | Routing, CORS, token validation, caller identity |
| `user-service` | 8081 | Driver profiles keyed by Keycloak subject |
| `discovery-service` | 8082 | Station catalogue, spatial search, connector tariffs |
| `session-service` | 8083 | Reservations, charger codes, metering, session events |
| `billing-service` | 8085 | Stripe invoices, idempotent settlement |
| `notification-service` | 8086 | Email delivery driven off events |

Full detail, including sequence diagrams and the trust boundary, is in
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). The reasoning behind the
non-obvious choices, and what each one costs, is in
[docs/DECISIONS.md](docs/DECISIONS.md).

Three things worth calling out:

**One live reservation per connector is enforced twice.** A distributed lock
serialises attempts, and a partial unique index —
`UNIQUE (connector_id) WHERE active = true` — is the backstop that holds even
if Redis is down or two instances race.

**Identity is set at the gateway, never by the client.** The gateway strips any
inbound `X-User-Id` and writes the validated token's subject in its place.
Without the strip, a driver with a valid token could read another driver's
reservations by sending a different id.

**Tariffs are snapshotted when a driver reserves.** Cost is energy times the
rate stored on the reservation, so a later price change cannot retroactively
rewrite a finished session.

## Development

Each service owns its own database and its own Flyway history. They must not
share one: two services migrating into the same database collide on schema
history version 1, and the second to start fails validation and exits.

```bash
mvn test              # per service; spotless:check runs at validate
mvn spotless:apply    # fix formatting

cd frontend
npm run format:check
npm run build
```

Formatting is enforced rather than maintained by hand — Spotless
(google-java-format, AOSP) for Java, Prettier for the front end. CI builds every
service, builds the front end, and validates the k8s manifests against
published Kubernetes schemas with kubeconform.

## Known gaps

- No integration tests against real PostGIS or Redis; coverage is unit-level.
- The payment screen is a mock; card capture is not wired to Stripe Elements.
- `notification-service` consumes a fixed set of event types that does not yet
  include `ChargingSessionCompleted`, and session events carry no recipient
  address, so email is not sent end to end.
- Session event delivery is at-most-once; a transactional outbox is the fix.
- The simulator's operator key is a shared secret shipped to the browser. Real
  hardware would use its own client-credentials token.

## Security

Real secrets belong only in local environment files or deployment secret stores.
Never commit Stripe, Resend, Keycloak, or database credentials.
