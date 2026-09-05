-- Folds the charger-code handshake (previously held in memory by
-- station-core-service) into the durable session schema.
--
-- Two columns per code: the bcrypt hash used to verify what a driver types,
-- and the plaintext the operator display shows. In production the plaintext
-- lives on the charger itself and never reaches this database; it is stored
-- here only because the simulator stands in for that hardware.

ALTER TABLE reservations
    ADD COLUMN start_code_hash VARCHAR(72) NOT NULL DEFAULT '',
    ADD COLUMN start_code_display VARCHAR(6) NOT NULL DEFAULT '',
    ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN rate_per_kwh NUMERIC(19, 6) NOT NULL DEFAULT 0,
    ADD COLUMN session_id UUID;

ALTER TABLE reservations
    ALTER COLUMN start_code_hash DROP DEFAULT,
    ALTER COLUMN start_code_display DROP DEFAULT,
    ALTER COLUMN rate_per_kwh DROP DEFAULT,
    ADD CONSTRAINT ck_reservations_rate_non_negative CHECK (rate_per_kwh >= 0),
    ADD CONSTRAINT ck_reservations_attempts_non_negative CHECK (failed_attempts >= 0);

ALTER TABLE charging_sessions
    ADD COLUMN stop_code_hash VARCHAR(72) NOT NULL DEFAULT '',
    ADD COLUMN stop_code_display VARCHAR(6) NOT NULL DEFAULT '',
    ADD COLUMN station_id VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN rate_per_kwh NUMERIC(19, 6) NOT NULL DEFAULT 0;

ALTER TABLE charging_sessions
    ALTER COLUMN stop_code_hash DROP DEFAULT,
    ALTER COLUMN stop_code_display DROP DEFAULT,
    ALTER COLUMN station_id DROP DEFAULT,
    ALTER COLUMN rate_per_kwh DROP DEFAULT,
    ADD CONSTRAINT ck_charging_sessions_rate_non_negative CHECK (rate_per_kwh >= 0);

-- The simulator lists everything currently live at one station.
CREATE INDEX ix_reservations_station ON reservations (station_id);
CREATE INDEX ix_charging_sessions_station ON charging_sessions (station_id);
CREATE INDEX ix_charging_sessions_owner ON charging_sessions (owner_id);
CREATE INDEX ix_reservations_owner ON reservations (owner_id);
