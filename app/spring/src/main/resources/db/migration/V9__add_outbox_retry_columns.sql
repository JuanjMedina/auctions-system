-- Soporte de reintentos para el patrón outbox: eventos que fallan al publicarse
-- se reintentan hasta un máximo; el último error queda registrado para diagnóstico.
ALTER TABLE outbox_events
    ADD COLUMN retry_count INT  NOT NULL DEFAULT 0,
    ADD COLUMN last_error  TEXT;
