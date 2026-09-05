-- Connector tariffs live with the station catalogue: discovery-service owns
-- what a connector costs, and session-service snapshots the rate onto a
-- reservation so a later price change never rewrites a finished session.
ALTER TABLE connectors ADD COLUMN rate_per_kwh NUMERIC(10, 4) NOT NULL DEFAULT 0.4000;

ALTER TABLE connectors ADD CONSTRAINT ck_connectors_rate_non_negative CHECK (rate_per_kwh >= 0);

UPDATE connectors SET rate_per_kwh = 0.4200 WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
UPDATE connectors SET rate_per_kwh = 0.2900 WHERE id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
UPDATE connectors SET rate_per_kwh = 0.4900 WHERE id = 'cccccccc-cccc-cccc-cccc-cccccccccccc';
UPDATE connectors SET rate_per_kwh = 0.3900 WHERE id = 'dddddddd-dddd-dddd-dddd-dddddddddddd';
