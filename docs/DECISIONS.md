# Design decisions

Short records of choices that were not obvious, and what they cost.

## 1. One database per service

**Context.** Every service ships its own Flyway migrations, but compose pointed
them all at a single database.

**Problem.** Flyway records version 1 with the first service's description and
checksum. Every service that starts after the first fails validation and exits.
Reproduced against PostgreSQL 16: `user-service` applies, then `discovery`,
`session`, `billing` and `notification` each fail with `FlywayValidateException`.

**Decision.** One database per service, created by an init script on first start.

**Cost.** Cross-service joins are impossible by construction. That is the point,
but it means reporting queries have to go through APIs or a downstream store.

## 2. Identity is set at the gateway, never by the client

**Context.** Downstream services read the driver from `X-User-Id`.

**Decision.** The gateway strips any inbound `X-User-Id` and writes the validated
token's subject in its place. Downstream services reject requests without it
rather than defaulting to an anonymous identity.

**Why the strip matters.** Validating the token alone is not enough. A driver
with a valid token for their own account could otherwise read another driver's
reservations simply by sending a different id.

**Cost.** Services now trust a header, which is only sound while the gateway is
the sole ingress. Exposing a service directly would break the assumption, so this
is a deployment invariant, not just a code one.

## 3. Two layers guard one live reservation per connector

**Decision.** An application-level distributed lock *and* a partial unique index
on `reservations (connector_id) WHERE active = true`.

**Why both.** The lock keeps the common case from ever reaching a constraint
violation, which keeps the error path rare and the user-facing behaviour clean.
The index is what actually guarantees the invariant: it holds when Redis is
down, when the lock is misconfigured, and when two instances race.

**Cost.** The invariant is expressed in two places and they must agree. The index
is authoritative; the lock is an optimisation.

## 4. Tariffs are snapshotted at reservation time

**Context.** `discovery-service` owns connector pricing; `session-service`
computes cost.

**Decision.** Read the rate once when the driver reserves and store it on the
reservation. Cost is energy times that stored rate.

**Why.** Looking the tariff up again at settlement would let a price change
retroactively rewrite what a driver already agreed to pay.

**Cost.** A synchronous call from `session-service` to `discovery-service` on the
reserve path, with its own timeouts and a 503 when the catalogue is unreachable.

## 5. Session events publish after commit

**Decision.** Completion is raised as an in-process event and relayed to RabbitMQ
by a listener bound to `AFTER_COMMIT`. Publish failures are logged, not rethrown.

**Why.** Publishing inside the transaction meant a broker problem rolled back a
charging session the driver had already finished. Settlement state matters more
than notification.

**Cost.** Delivery is at-most-once: a crash between commit and publish loses the
event. A transactional outbox is the correct fix and is not implemented.

**Also.** The producer declares the queue. Publishing to the default exchange
with a routing key matching no queue is silently dropped by RabbitMQ, so relying
on the consumer having started first loses messages with no error anywhere.

## 6. Charger codes are hashed, with an attempt budget

**Decision.** Six-digit codes, bcrypt-hashed, with a per-reservation failed
attempt limit.

**Why.** A six-digit space is small. Hashing protects a database dump; the
attempt budget is what actually bounds an online attacker. Neither alone is
enough.

**Cost.** The plaintext is also stored so the simulator can display it. In
production that value lives on the charger and never reaches this database.

## 7. Tokens in localStorage, not sessionStorage

**Decision.** `localStorage`, with short-lived access tokens and silent renew.

**Why.** `sessionStorage` is per-tab. The operator simulator is opened in a
second tab, and a session stored per-tab forced a fresh sign-in for every tab,
which broke the documented workflow.

**Cost.** Tokens outlive the tab and are readable by script on the origin. Short
lifetimes and silent renew bound the exposure; a stricter deployment would move
to refresh tokens held in a backend-for-frontend.

## 8. CORS is configured once, in Spring Security

**Context.** The gateway had CORS configured via Spring Cloud Gateway's
`globalcors`, and Spring Security required authentication on every exchange.

**Problem.** A CORS preflight carries no `Authorization` header, so it was
rejected before CORS ran. Every cross-origin call from the SPA failed.

**Decision.** Permit `OPTIONS` preflight, and configure CORS only in the security
chain — not in both places, because two layers each adding
`Access-Control-Allow-Origin` produces duplicate headers that browsers reject.
