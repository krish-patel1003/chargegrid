CREATE TABLE reservations (id UUID PRIMARY KEY, owner_id VARCHAR(255) NOT NULL, station_id VARCHAR(255) NOT NULL, connector_id VARCHAR(255) NOT NULL, status VARCHAR(32) NOT NULL, expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL, active BOOLEAN NOT NULL);
CREATE UNIQUE INDEX uk_active_connector ON reservations(connector_id) WHERE active = true;
CREATE TABLE charging_sessions (id UUID PRIMARY KEY, reservation_id UUID NOT NULL UNIQUE REFERENCES reservations(id), owner_id VARCHAR(255) NOT NULL, connector_id VARCHAR(255) NOT NULL, started_at TIMESTAMPTZ NOT NULL, completed_at TIMESTAMPTZ, energy_kwh NUMERIC(19,6) NOT NULL, status VARCHAR(32) NOT NULL);
CREATE TABLE meter_readings (id UUID PRIMARY KEY, session_id UUID NOT NULL REFERENCES charging_sessions(id), kwh NUMERIC(19,6) NOT NULL, recorded_at TIMESTAMPTZ NOT NULL);
CREATE INDEX ix_meter_session ON meter_readings(session_id, recorded_at);
