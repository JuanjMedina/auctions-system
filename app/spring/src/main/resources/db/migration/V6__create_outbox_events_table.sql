CREATE TABLE outbox_events
(
    id             UUID         PRIMARY KEY,
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB        NOT NULL,
    processed      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    processed_at   TIMESTAMPTZ
);

-- índice parcial: solo eventos no procesados — muy eficiente para el OutboxPoller
CREATE INDEX idx_outbox_unprocessed ON outbox_events (created_at ASC) WHERE processed = FALSE;
CREATE INDEX idx_outbox_aggregate   ON outbox_events (aggregate_type, aggregate_id);
