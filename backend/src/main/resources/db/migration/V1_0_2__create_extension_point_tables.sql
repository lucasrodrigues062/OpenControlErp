-- V1_0_2__create_extension_point_tables.sql
-- Table for tracking SPI provider registrations by plugins

CREATE TABLE IF NOT EXISTS extension_point_registration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_fk UUID NOT NULL REFERENCES extension_plugin(id) ON DELETE CASCADE,
    extension_point_interface VARCHAR(255) NOT NULL,
    implementation_class VARCHAR(255) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 100,
    context_constraints JSONB,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_epr_interface ON extension_point_registration(extension_point_interface);
CREATE INDEX IF NOT EXISTS idx_epr_active ON extension_point_registration(active);
