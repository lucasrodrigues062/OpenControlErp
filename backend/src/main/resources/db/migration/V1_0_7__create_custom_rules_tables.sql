CREATE TABLE custom_rule (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    trigger_event VARCHAR(100) NOT NULL,
    timing VARCHAR(20) NOT NULL DEFAULT 'BEFORE',
    authoring_mode VARCHAR(30) NOT NULL DEFAULT 'VISUAL_BUILDER',
    condition_expression TEXT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_payload TEXT,
    priority INT NOT NULL DEFAULT 100,
    is_active BOOLEAN NOT NULL DEFAULT true,
    error_handling_policy VARCHAR(30) NOT NULL DEFAULT 'ABORT_TRANSACTION',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_custom_rule_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_custom_rule_tenant_trigger 
    ON custom_rule (tenant_id, trigger_event, is_active, priority ASC);

CREATE TABLE rule_execution_trace (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    rule_id UUID NOT NULL,
    trigger_event VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    duration_ms BIGINT NOT NULL,
    result VARCHAR(30) NOT NULL,
    error_detail TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rule_execution_trace_tenant_rule 
    ON rule_execution_trace (tenant_id, rule_id, created_at DESC);
CREATE INDEX idx_rule_execution_trace_tenant_created 
    ON rule_execution_trace (tenant_id, created_at DESC);
