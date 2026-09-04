CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE stations (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    location geography(Point, 4326) NOT NULL
);

CREATE TABLE connectors (
    id UUID PRIMARY KEY,
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    connector_type VARCHAR(40) NOT NULL,
    power_kw INTEGER NOT NULL CHECK (power_kw > 0),
    available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_stations_location ON stations USING GIST (location);
CREATE INDEX idx_connectors_station_type_available ON connectors (station_id, connector_type, available);

INSERT INTO stations (id, name, address, latitude, longitude, location) VALUES
('11111111-1111-1111-1111-111111111111', 'Long Beach Civic Center', '333 W Ocean Blvd, Long Beach, CA', 33.7683, -118.1956, ST_SetSRID(ST_MakePoint(-118.1956, 33.7683), 4326)::geography),
('22222222-2222-2222-2222-222222222222', 'Long Beach Marina', '200 Aquarium Way, Long Beach, CA', 33.7636, -118.1890, ST_SetSRID(ST_MakePoint(-118.1890, 33.7636), 4326)::geography);

INSERT INTO connectors (id, station_id, connector_type, power_kw, available) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'CCS', 150, TRUE),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'J1772', 11, TRUE),
('cccccccc-cccc-cccc-cccc-cccccccccccc', '22222222-2222-2222-2222-222222222222', 'CCS', 50, FALSE),
('dddddddd-dddd-dddd-dddd-dddddddddddd', '22222222-2222-2222-2222-222222222222', 'CHAdeMO', 50, TRUE);
