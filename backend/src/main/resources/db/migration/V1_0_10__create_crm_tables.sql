-- Migration: V1_0_10__create_crm_tables.sql
-- Description: Create CRM tables (Customer Categories, Economic Groups, Customers, Leads, Sales Pipelines, Pipeline Stages, Deals, and CRM Activities) and seed CRM permissions

-- 1. Customer Categories (Segmentation Taxonomy)
CREATE TABLE customer_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    color_hex VARCHAR(7) DEFAULT '#3B82F6',
    default_discount_pct NUMERIC(5, 2) DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_customer_categories_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_customer_categories_tenant ON customer_categories(tenant_id);

-- 2. Economic Groups (Corporate Holding Conglomerates)
CREATE TABLE economic_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    holding_cnpj VARCHAR(14),
    consolidated_credit_limit NUMERIC(15, 2) DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_economic_groups_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_economic_groups_tenant ON economic_groups(tenant_id);

-- 3. Customers (Master Commercial Records: PF / PJ)
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    branch_id UUID REFERENCES company_branches(id) ON DELETE SET NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'LEGAL_ENTITY', -- 'INDIVIDUAL', 'LEGAL_ENTITY'
    document VARCHAR(14) NOT NULL,
    legal_name VARCHAR(150) NOT NULL,
    trade_name VARCHAR(150),
    state_registration VARCHAR(30),
    is_state_registration_exempt BOOLEAN NOT NULL DEFAULT FALSE,
    municipal_registration VARCHAR(30),
    icms_taxpayer_type VARCHAR(30) NOT NULL DEFAULT 'NON_CONTRIBUTOR', -- 'CONTRIBUTOR', 'NON_CONTRIBUTOR', 'EXEMPT'
    main_cnae VARCHAR(20),
    birth_or_founding_date DATE,
    phone_1 VARCHAR(30),
    phone_2 VARCHAR(30),
    email VARCHAR(100),
    contact_person VARCHAR(100),
    website VARCHAR(200),
    category_id UUID REFERENCES customer_categories(id) ON DELETE SET NULL,
    economic_group_id UUID REFERENCES economic_groups(id) ON DELETE SET NULL,
    credit_limit NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    credit_status VARCHAR(30) NOT NULL DEFAULT 'APPROVED', -- 'APPROVED', 'SUSPENDED', 'BLOCKED'
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Primary Statutory Address
    postal_code VARCHAR(10),
    street VARCHAR(150),
    number VARCHAR(30),
    complement VARCHAR(100),
    neighborhood VARCHAR(100),
    city_name VARCHAR(100),
    city_ibge_code INTEGER,
    state_code VARCHAR(2),
    country_code VARCHAR(3) NOT NULL DEFAULT 'BRA',

    -- Billing Address (Cobrança)
    billing_postal_code VARCHAR(10),
    billing_street VARCHAR(150),
    billing_number VARCHAR(30),
    billing_complement VARCHAR(100),
    billing_neighborhood VARCHAR(100),
    billing_city_name VARCHAR(100),
    billing_state_code VARCHAR(2),

    -- Delivery Address (Entrega)
    delivery_postal_code VARCHAR(10),
    delivery_street VARCHAR(150),
    delivery_number VARCHAR(30),
    delivery_complement VARCHAR(100),
    delivery_neighborhood VARCHAR(100),
    delivery_city_name VARCHAR(100),
    delivery_state_code VARCHAR(2),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_customers_tenant_document UNIQUE (tenant_id, document)
);

CREATE INDEX idx_customers_tenant ON customers(tenant_id);
CREATE INDEX idx_customers_branch ON customers(branch_id);
CREATE INDEX idx_customers_category ON customers(category_id);
CREATE INDEX idx_customers_economic_group ON customers(economic_group_id);
CREATE INDEX idx_customers_document ON customers(document);
CREATE INDEX idx_customers_legal_name ON customers(legal_name);
CREATE INDEX idx_customers_trade_name ON customers(trade_name);

-- 4. Leads (Prospects)
CREATE TABLE leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    branch_id UUID REFERENCES company_branches(id) ON DELETE SET NULL,
    name VARCHAR(150) NOT NULL,
    company_name VARCHAR(150),
    email VARCHAR(100),
    phone VARCHAR(30),
    source VARCHAR(50) NOT NULL DEFAULT 'WEBSITE',
    status VARCHAR(30) NOT NULL DEFAULT 'NEW', -- 'NEW', 'IN_QUALIFICATION', 'QUALIFIED', 'DISQUALIFIED', 'CONVERTED'
    estimated_budget NUMERIC(15, 2) DEFAULT 0.00,
    interest_summary TEXT,
    assigned_user_id UUID REFERENCES iam_user(id) ON DELETE SET NULL,
    disqualification_reason VARCHAR(255),
    converted_customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    converted_deal_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leads_tenant ON leads(tenant_id);
CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_assigned_user ON leads(assigned_user_id);

-- 5. Sales Pipelines
CREATE TABLE sales_pipelines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sales_pipelines_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_sales_pipelines_tenant ON sales_pipelines(tenant_id);

-- 6. Pipeline Stages
CREATE TABLE pipeline_stages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pipeline_id UUID NOT NULL REFERENCES sales_pipelines(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    order_index INTEGER NOT NULL DEFAULT 0,
    win_probability_pct INTEGER NOT NULL DEFAULT 10,
    color_hex VARCHAR(7) DEFAULT '#6366F1',
    max_stagnant_days INTEGER DEFAULT 15,
    stage_type VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' -- 'IN_PROGRESS', 'WON', 'LOST'
);

CREATE INDEX idx_pipeline_stages_pipeline ON pipeline_stages(pipeline_id, order_index);

-- 7. Deals (Opportunities)
CREATE TABLE deals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    branch_id UUID REFERENCES company_branches(id) ON DELETE SET NULL,
    pipeline_id UUID NOT NULL REFERENCES sales_pipelines(id) ON DELETE RESTRICT,
    stage_id UUID NOT NULL REFERENCES pipeline_stages(id) ON DELETE RESTRICT,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    lead_id UUID REFERENCES leads(id) ON DELETE SET NULL,
    title VARCHAR(150) NOT NULL,
    estimated_value NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    expected_close_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- 'OPEN', 'WON', 'LOST'
    loss_reason VARCHAR(255),
    assigned_user_id UUID REFERENCES iam_user(id) ON DELETE SET NULL,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_deals_tenant ON deals(tenant_id);
CREATE INDEX idx_deals_stage ON deals(stage_id);
CREATE INDEX idx_deals_customer ON deals(customer_id);
CREATE INDEX idx_deals_assigned_user ON deals(assigned_user_id);
CREATE INDEX idx_deals_status ON deals(status);

-- 8. CRM Activities (Timeline History)
CREATE TABLE crm_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    entity_type VARCHAR(30) NOT NULL, -- 'CUSTOMER', 'LEAD', 'DEAL'
    entity_id UUID NOT NULL,
    activity_type VARCHAR(30) NOT NULL, -- 'NOTE', 'CALL', 'MEETING', 'WHATSAPP', 'TASK'
    summary VARCHAR(200) NOT NULL,
    description TEXT,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    due_date TIMESTAMPTZ,
    is_completed BOOLEAN NOT NULL DEFAULT TRUE,
    author_user_id UUID REFERENCES iam_user(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crm_activities_target ON crm_activities(tenant_id, entity_type, entity_id);
CREATE INDEX idx_crm_activities_performed ON crm_activities(performed_at DESC);

-- 9. Seed CRM Permissions for Administrator Profile
INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'CRM_READ' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;

INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'CRM_WRITE' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;

INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'CRM_ADMIN' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;
