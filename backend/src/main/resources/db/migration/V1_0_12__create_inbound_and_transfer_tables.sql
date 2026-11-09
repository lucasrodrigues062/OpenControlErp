-- ==============================================================================
-- OpenControl ERP - Inbound Procurement, Invoicing & Transfers Schema
-- Migration V1_0_12
-- ==============================================================================

-- 1. Cost Formula Profiles (Excel-like dynamic landed cost engine)
CREATE TABLE cost_formula_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    formula_expression TEXT NOT NULL,
    freight_proration_method VARCHAR(30) NOT NULL DEFAULT 'VALUE_PROPORTIONAL', -- VALUE_PROPORTIONAL, WEIGHT_PROPORTIONAL
    custom_variables JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cost_formula_profiles_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_cost_formula_tenant ON cost_formula_profiles(tenant_id);

-- 2. Supplier Product De-Para Mapping & Unit Conversion Memory
CREATE TABLE supplier_product_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    supplier_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    supplier_item_code VARCHAR(100) NOT NULL,
    supplier_barcode VARCHAR(50),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    supplier_uom VARCHAR(10) NOT NULL DEFAULT 'UN',
    conversion_factor NUMERIC(15, 4) NOT NULL DEFAULT 1.0000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_supplier_product_mapping UNIQUE (tenant_id, supplier_id, supplier_item_code)
);

CREATE INDEX idx_supplier_mapping_lookup ON supplier_product_mappings(tenant_id, supplier_id, supplier_item_code);

-- 3. Purchase Orders to Suppliers
CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    branch_id UUID NOT NULL REFERENCES company_branches(id) ON DELETE RESTRICT,
    order_number VARCHAR(50) NOT NULL,
    supplier_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    order_date DATE NOT NULL,
    expected_delivery_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, APPROVED, PARTIALLY_RECEIVED, FULLY_RECEIVED, CANCELLED
    total_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    payment_terms VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_purchase_order_num UNIQUE (tenant_id, order_number)
);

CREATE INDEX idx_purchase_orders_tenant_status ON purchase_orders(tenant_id, status);
CREATE INDEX idx_purchase_orders_supplier ON purchase_orders(supplier_id);

-- 4. Purchase Order Items
CREATE TABLE purchase_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    target_warehouse_id UUID REFERENCES warehouses(id) ON DELETE RESTRICT,
    quantity_ordered NUMERIC(15, 4) NOT NULL,
    quantity_received NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    unit_price NUMERIC(15, 4) NOT NULL,
    discount_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_price NUMERIC(15, 4) NOT NULL
);

CREATE INDEX idx_purchase_order_items_order ON purchase_order_items(purchase_order_id);

-- 5. Inbound Invoices (Notas Fiscais de Entrada)
CREATE TABLE inbound_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    branch_id UUID NOT NULL REFERENCES company_branches(id) ON DELETE RESTRICT,
    supplier_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    cost_formula_profile_id UUID REFERENCES cost_formula_profiles(id) ON DELETE SET NULL,
    purchase_order_id UUID REFERENCES purchase_orders(id) ON DELETE SET NULL,
    access_key VARCHAR(44),
    invoice_number VARCHAR(30) NOT NULL,
    series VARCHAR(10) NOT NULL DEFAULT '1',
    issuance_date DATE NOT NULL,
    entry_date DATE NOT NULL DEFAULT CURRENT_DATE,
    total_products NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_freight NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_insurance NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_expenses NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_discount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_invoice_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    custom_variable_values JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PENDING_CONFERENCE, CONFERRED, STOCKED, CANCELLED
    raw_xml_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inbound_invoices_tenant_branch ON inbound_invoices(tenant_id, branch_id);
CREATE INDEX idx_inbound_invoices_access_key ON inbound_invoices(tenant_id, access_key);

-- 6. Inbound Invoice Items
CREATE TABLE inbound_invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    inbound_invoice_id UUID NOT NULL REFERENCES inbound_invoices(id) ON DELETE CASCADE,
    item_index INTEGER NOT NULL,
    supplier_item_code VARCHAR(100) NOT NULL,
    supplier_item_description VARCHAR(255) NOT NULL,
    matched_product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    supplier_uom VARCHAR(10) NOT NULL DEFAULT 'UN',
    internal_uom VARCHAR(10) NOT NULL DEFAULT 'UN',
    conversion_factor NUMERIC(15, 4) NOT NULL DEFAULT 1.0000,
    invoiced_quantity NUMERIC(15, 4) NOT NULL,
    internal_quantity NUMERIC(15, 4) NOT NULL,
    unit_price NUMERIC(15, 4) NOT NULL,
    total_price NUMERIC(15, 4) NOT NULL,
    allocated_freight NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    allocated_insurance NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    allocated_expenses NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    icms_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    icms_credit_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    ipi_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    pis_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    pis_credit_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    cofins_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    cofins_credit_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    final_landed_unit_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    final_total_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000
);

CREATE INDEX idx_inbound_invoice_items_invoice ON inbound_invoice_items(inbound_invoice_id);

-- 7. Goods Receipts (Physical Receiving Conference)
CREATE TABLE goods_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    branch_id UUID NOT NULL REFERENCES company_branches(id) ON DELETE RESTRICT,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    purchase_order_id UUID REFERENCES purchase_orders(id) ON DELETE SET NULL,
    inbound_invoice_id UUID REFERENCES inbound_invoices(id) ON DELETE SET NULL,
    receipt_number VARCHAR(50) NOT NULL,
    receipt_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, CONFERRED_OK, CONFERRED_WITH_DIVERGENCE, CANCELLED
    conference_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_goods_receipts_tenant ON goods_receipts(tenant_id, branch_id);

-- 8. Goods Receipt Items (Traceability & Discrepancies)
CREATE TABLE goods_receipt_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    goods_receipt_id UUID NOT NULL REFERENCES goods_receipts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    expected_quantity NUMERIC(15, 4) NOT NULL,
    received_quantity NUMERIC(15, 4) NOT NULL,
    rejected_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    lot_number VARCHAR(100),
    fabrication_date DATE,
    expiration_date DATE,
    serial_numbers_json JSONB NOT NULL DEFAULT '[]'::jsonb
);

CREATE INDEX idx_goods_receipt_items_receipt ON goods_receipt_items(goods_receipt_id);

-- 9. Stock Transfers (Internal Warehouse & Inter-Branch Transfers)
CREATE TABLE stock_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    transfer_number VARCHAR(50) NOT NULL,
    transfer_type VARCHAR(30) NOT NULL DEFAULT 'INTERNAL_WAREHOUSE', -- INTERNAL_WAREHOUSE, INTER_BRANCH
    origin_branch_id UUID NOT NULL REFERENCES company_branches(id) ON DELETE RESTRICT,
    origin_warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    destination_branch_id UUID NOT NULL REFERENCES company_branches(id) ON DELETE RESTRICT,
    destination_warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- DRAFT, DISPATCHED_IN_TRANSIT, RECEIVED_COMPLETED, CANCELLED
    dispatched_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ,
    tracking_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stock_transfer_num UNIQUE (tenant_id, transfer_number)
);

CREATE INDEX idx_stock_transfers_tenant ON stock_transfers(tenant_id, origin_branch_id, destination_branch_id);

-- 10. Stock Transfer Items
CREATE TABLE stock_transfer_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    stock_transfer_id UUID NOT NULL REFERENCES stock_transfers(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    requested_quantity NUMERIC(15, 4) NOT NULL,
    shipped_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    received_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    unit_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    lot_id UUID REFERENCES product_lots(id) ON DELETE SET NULL,
    serial_numbers_json JSONB NOT NULL DEFAULT '[]'::jsonb
);

CREATE INDEX idx_stock_transfer_items_transfer ON stock_transfer_items(stock_transfer_id);

-- 11. Seed Permissions
INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'INBOUND_READ' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;

INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'INBOUND_WRITE' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;

INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'TRANSFER_READ' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;

INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'TRANSFER_WRITE' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;
