-- Each service owns its own schema and its own Flyway history, so they need
-- separate databases. Pointing them all at one database makes the second
-- service to start fail: it finds version 1 already applied with a different
-- description and checksum.
--
-- Runs once, on first initialisation of an empty data volume.

CREATE DATABASE chargegrid_users;
CREATE DATABASE chargegrid_discovery;
CREATE DATABASE chargegrid_sessions;
CREATE DATABASE chargegrid_billing;
CREATE DATABASE chargegrid_notifications;

-- discovery-service stores station geography; its Flyway V1 enables PostGIS,
-- which requires the extension to be installable in that database.
\connect chargegrid_discovery
CREATE EXTENSION IF NOT EXISTS postgis;
