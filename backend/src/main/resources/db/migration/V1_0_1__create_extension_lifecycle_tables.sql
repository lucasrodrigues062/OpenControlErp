-- V1_0_1__create_extension_lifecycle_tables.sql
-- Tables for managing plugins, dependencies, and immutable audit log

CREATE TABLE IF NOT EXISTS extension_plugin (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_id VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    version VARCHAR(32) NOT NULL,
    vendor VARCHAR(120) NOT NULL,
    min_kernel_version VARCHAR(32) NOT NULL,
    max_kernel_version VARCHAR(32),
    status VARCHAR(30) NOT NULL,
    archive_checksum VARCHAR(64),
    file_path VARCHAR(500),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_plugin_status ON extension_plugin(status);

CREATE TABLE IF NOT EXISTS extension_dependency (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_fk UUID NOT NULL REFERENCES extension_plugin(id) ON DELETE CASCADE,
    depends_on_plugin_id VARCHAR(120) NOT NULL,
    version_range VARCHAR(64) NOT NULL,
    optional BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_dependency_plugin_fk ON extension_dependency(plugin_fk);

CREATE TABLE IF NOT EXISTS extension_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_fk UUID REFERENCES extension_plugin(id) ON DELETE SET NULL,
    action VARCHAR(40) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_plugin_fk ON extension_audit_log(plugin_fk);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON extension_audit_log(created_at);
