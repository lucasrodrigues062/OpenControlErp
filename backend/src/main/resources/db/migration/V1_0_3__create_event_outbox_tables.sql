-- V1_0_3__create_event_outbox_tables.sql
-- Transactional Outbox pattern for asynchronous event reliability and fault isolation

CREATE TABLE IF NOT EXISTS event_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(80) NOT NULL,
    tenant_id UUID NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_retry ON event_outbox(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_outbox_tenant_id ON event_outbox(tenant_id);

CREATE TABLE IF NOT EXISTS event_delivery_attempt (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outbox_fk UUID NOT NULL REFERENCES event_outbox(id) ON DELETE CASCADE,
    listener_identifier VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_trace TEXT,
    duration_ms INTEGER NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_eda_outbox_fk ON event_delivery_attempt(outbox_fk);
