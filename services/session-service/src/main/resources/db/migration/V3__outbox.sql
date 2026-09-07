-- Transactional outbox.
--
-- The event is written in the same transaction as the session it describes, so
-- committing the session and recording the intent to publish either both happen
-- or neither does. A relay then moves rows to the broker.
--
-- This turns delivery from at-most-once (publish after commit, lost on a crash)
-- into at-least-once: a crash between publishing and marking the row published
-- replays it, so consumers must be idempotent.

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    CONSTRAINT ck_outbox_attempts_non_negative CHECK (attempts >= 0)
);

-- The relay only ever scans unpublished rows, and this keeps that scan off the
-- published ones, which is where the volume ends up.
CREATE INDEX ix_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
