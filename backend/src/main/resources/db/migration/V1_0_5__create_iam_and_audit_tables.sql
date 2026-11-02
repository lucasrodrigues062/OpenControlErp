-- Migration: V1_0_5__create_iam_and_audit_tables.sql
-- Description: Create IAM (Profiles, Permissions, Users) and Audit Log tables

-- 1. Access Profiles (Roles)
CREATE TABLE iam_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_iam_profile_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_iam_profile_tenant ON iam_profile (tenant_id);

-- 2. Profile Permissions (Normalized Granular Permissions)
CREATE TABLE iam_profile_permission (
    profile_id UUID NOT NULL REFERENCES iam_profile(id) ON DELETE CASCADE,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (profile_id, permission)
);

CREATE INDEX idx_iam_profile_permission_profile ON iam_profile_permission (profile_id);

-- 3. Users (Strict Single-Profile Assignment per User)
CREATE TABLE iam_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    profile_id UUID NOT NULL REFERENCES iam_profile(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_iam_user_tenant_username UNIQUE (tenant_id, username),
    CONSTRAINT uq_iam_user_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX idx_iam_user_tenant ON iam_user (tenant_id);
CREATE INDEX idx_iam_user_profile ON iam_user (profile_id);

-- 4. Unified Audit Log (Immutable, Append-Only)
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    actor_id UUID,
    actor_username VARCHAR(100) NOT NULL,
    action VARCHAR(30) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    details TEXT,
    state_before JSONB,
    state_after JSONB,
    client_ip VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_tenant_created ON audit_log (tenant_id, created_at DESC);
CREATE INDEX idx_audit_log_entity ON audit_log (tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_log_actor ON audit_log (tenant_id, actor_id, created_at DESC);
