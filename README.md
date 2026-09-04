# ChargeGrid

ChargeGrid is a demo EV charging journey: authenticate, discover a nearby station, reserve a connector, verify charger codes, simulate a charging session, and settle the session through Stripe.

## Current development slice

The first runnable slice includes the discovery experience, station details, reservation/session/activity/payment screens, charger simulator, PostgreSQL/PostGIS, Redis, RabbitMQ, and Keycloak development infrastructure. Payment and identity screens use demo adapters until their service endpoints are connected.

## Run locally

1. Copy `.env.example` to `.env` and replace local values as needed.
2. Start infrastructure with `docker compose up -d`.
3. Start the frontend with `cd frontend`, `npm install`, and `npm run dev`.
4. Open `http://localhost:5173/login`.

Keycloak is available at `http://localhost:8080`, RabbitMQ management at `http://localhost:15672`, and PostgreSQL/PostGIS at port `5432`.

## Security

Real secrets belong only in local environment files or deployment secret stores. Never commit Stripe, Resend, Keycloak, JWT, or database credentials.
