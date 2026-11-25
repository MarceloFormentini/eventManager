CREATE TABLE processed_event (
    event_id BIGINT PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE event
    ADD CONSTRAINT exclusion_event_location_active_times
    EXCLUDE USING gist (
        location_id WITH =,
        tstzrange(start_date, end_date, '[)') WITH &&
    )
    WHERE (status IN ('DRAFT', 'PUBLISHED'));
