-- V1_0_8: Create tables for dynamic custom fields engine

CREATE TABLE custom_field_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    target_entity VARCHAR(64) NOT NULL,
    field_key VARCHAR(64) NOT NULL,
    label VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    field_type VARCHAR(32) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    admin_only BOOLEAN NOT NULL DEFAULT FALSE,
    read_only BOOLEAN NOT NULL DEFAULT FALSE,
    default_value VARCHAR(255),
    options JSONB,
    validation_rules JSONB,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tenant_entity_field_key UNIQUE (tenant_id, target_entity, field_key)
);

CREATE INDEX idx_custom_field_defs_lookup 
    ON custom_field_definitions (tenant_id, target_entity, is_active, display_order);

CREATE TABLE entity_custom_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    target_entity VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    field_values JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_tenant_target_entity_record UNIQUE (tenant_id, target_entity, entity_id)
);

CREATE INDEX idx_entity_custom_fields_lookup 
    ON entity_custom_fields (tenant_id, target_entity, entity_id);

CREATE INDEX idx_entity_custom_fields_gin 
    ON entity_custom_fields USING GIN (field_values);
