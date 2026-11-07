-- Migration: V1_0_11__create_product_and_inventory_tables.sql
-- Description: Create Product Master Data, Pricing Tables, Multi-Warehouse Inventory, Traceability (Lots & Serials) and Kardex Movement tables

-- 1. Product Categories (Hierarchical Taxonomy)
CREATE TABLE product_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    parent_id UUID REFERENCES product_categories(id) ON DELETE SET NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    color_hex VARCHAR(7) NOT NULL DEFAULT '#10B981',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_categories_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_product_categories_tenant ON product_categories(tenant_id);
CREATE INDEX idx_product_categories_parent ON product_categories(parent_id);

-- 2. Products (Master Items & Fiscal Compliance)
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    branch_id UUID REFERENCES company_branches(id) ON DELETE SET NULL,
    sku VARCHAR(50) NOT NULL,
    barcode VARCHAR(50),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    item_type VARCHAR(20) NOT NULL DEFAULT 'REV_00',
    unit_of_measure VARCHAR(10) NOT NULL DEFAULT 'UN',
    category_id UUID NOT NULL REFERENCES product_categories(id) ON DELETE RESTRICT,
    brand VARCHAR(100),
    model VARCHAR(100),
    ncm VARCHAR(8) NOT NULL,
    ncm_exception VARCHAR(3),
    cest VARCHAR(7),
    origin INT NOT NULL DEFAULT 0,
    tracking_mode VARCHAR(20) NOT NULL DEFAULT 'NONE',
    gross_weight NUMERIC(12, 4),
    net_weight NUMERIC(12, 4),
    width NUMERIC(10, 2),
    height NUMERIC(10, 2),
    depth NUMERIC(10, 2),
    volume NUMERIC(12, 4),
    min_stock NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    max_stock NUMERIC(15, 4),
    reorder_point NUMERIC(15, 4),
    last_acquisition_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    average_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    replacement_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_products_tenant_sku UNIQUE (tenant_id, sku)
);

CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE UNIQUE INDEX uq_products_tenant_barcode ON products(tenant_id, barcode) WHERE barcode IS NOT NULL;
CREATE INDEX idx_products_tenant_name ON products(tenant_id, name);
CREATE INDEX idx_products_tenant_ncm ON products(tenant_id, ncm);
CREATE INDEX idx_products_category ON products(category_id);

-- 3. Product Unit Conversions (Packaging & Purchasing factors)
CREATE TABLE product_unit_conversions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    unit_from VARCHAR(10) NOT NULL,
    unit_to VARCHAR(10) NOT NULL,
    conversion_factor NUMERIC(15, 6) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_unit_conversion UNIQUE (product_id, unit_from, unit_to)
);

CREATE INDEX idx_product_unit_conversions_product ON product_unit_conversions(product_id);

-- 4. Warehouses (Physical / Virtual Storage Locations partitioned by Branch)
CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    branch_id UUID NOT NULL REFERENCES company_branches(id) ON DELETE RESTRICT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    warehouse_type VARCHAR(30) NOT NULL DEFAULT 'COMMERCIAL',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_warehouses_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_warehouses_tenant ON warehouses(tenant_id);
CREATE INDEX idx_warehouses_branch ON warehouses(branch_id);

-- 5. Storage Locations (Physical Bin Addressing: Aisle-Rack-Shelf-Bin)
CREATE TABLE storage_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    aisle VARCHAR(20) NOT NULL,
    rack VARCHAR(20) NOT NULL,
    shelf VARCHAR(20) NOT NULL,
    bin VARCHAR(20) NOT NULL,
    formatted_address VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_storage_locations_warehouse_address UNIQUE (warehouse_id, formatted_address)
);

CREATE INDEX idx_storage_locations_warehouse ON storage_locations(warehouse_id);

-- 6. Stock Balances (Current stock position on hand, reserved, available)
CREATE TABLE stock_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    location_id UUID REFERENCES storage_locations(id) ON DELETE SET NULL,
    quantity_on_hand NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    quantity_reserved NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    quantity_available NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_stock_balances_loc ON stock_balances(warehouse_id, product_id, COALESCE(location_id, '00000000-0000-0000-0000-000000000000'::uuid));
CREATE INDEX idx_stock_balances_tenant_product ON stock_balances(tenant_id, product_id);
CREATE INDEX idx_stock_balances_warehouse_product ON stock_balances(warehouse_id, product_id);

-- 7. Product Lots (Batches with Manufacturing, Expiration & FEFO)
CREATE TABLE product_lots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    lot_number VARCHAR(100) NOT NULL,
    manufacturing_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    quantity_on_hand NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    quantity_reserved NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(30) NOT NULL DEFAULT 'RELEASED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_lots_tenant_prod_lot UNIQUE (tenant_id, product_id, lot_number)
);

CREATE INDEX idx_product_lots_product_expiry ON product_lots(product_id, expiration_date ASC) WHERE status = 'RELEASED';
CREATE INDEX idx_product_lots_tenant_warehouse ON product_lots(tenant_id, warehouse_id);

-- 8. Product Serial Numbers (Unit-Level Traceability)
CREATE TABLE product_serial_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    lot_id UUID REFERENCES product_lots(id) ON DELETE SET NULL,
    serial_number VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_STOCK',
    warranty_until DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_serials_tenant_prod_serial UNIQUE (tenant_id, product_id, serial_number)
);

CREATE INDEX idx_product_serials_lookup ON product_serial_numbers(tenant_id, serial_number);
CREATE INDEX idx_product_serials_product_status ON product_serial_numbers(product_id, status);

-- 9. Price Lists (Commercial Price Policies)
CREATE TABLE price_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    discount_or_markup_pct NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from DATE,
    valid_until DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_price_lists_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_price_lists_tenant ON price_lists(tenant_id);

-- 10. Product Prices (Specific Prices per List and Promotional Schedules)
CREATE TABLE product_prices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    price_list_id UUID NOT NULL REFERENCES price_lists(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sales_price NUMERIC(15, 4) NOT NULL,
    promotional_price NUMERIC(15, 4),
    promo_start DATE,
    promo_end DATE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_product_prices_list_prod UNIQUE (price_list_id, product_id)
);

CREATE INDEX idx_product_prices_list ON product_prices(price_list_id);
CREATE INDEX idx_product_prices_product ON product_prices(product_id);

-- 11. Stock Movements (Immutable Kardex Double-Entry Ledger)
CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    branch_id UUID NOT NULL REFERENCES company_branches(id) ON DELETE RESTRICT,
    movement_type VARCHAR(30) NOT NULL,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    source_warehouse_id UUID REFERENCES warehouses(id) ON DELETE RESTRICT,
    destination_warehouse_id UUID REFERENCES warehouses(id) ON DELETE RESTRICT,
    location_id UUID REFERENCES storage_locations(id) ON DELETE SET NULL,
    lot_id UUID REFERENCES product_lots(id) ON DELETE SET NULL,
    serial_number_id UUID REFERENCES product_serial_numbers(id) ON DELETE SET NULL,
    quantity NUMERIC(15, 4) NOT NULL,
    unit_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    balance_after NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    reference_document VARCHAR(100),
    reason_code VARCHAR(50),
    notes TEXT,
    created_by_user_id UUID REFERENCES iam_user(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_kardex ON stock_movements(tenant_id, product_id, created_at DESC);
CREATE INDEX idx_stock_movements_source ON stock_movements(source_warehouse_id);
CREATE INDEX idx_stock_movements_dest ON stock_movements(destination_warehouse_id);

-- 12. Seed Catalog & Inventory Permissions for Administrator Profile
INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'CATALOG_READ' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;

INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'CATALOG_WRITE' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;

INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'INVENTORY_READ' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;

INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'INVENTORY_WRITE' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;

INSERT INTO iam_profile_permission (profile_id, permission)
SELECT id, 'INVENTORY_ADMIN' FROM iam_profile WHERE name = 'Administrator'
ON CONFLICT DO NOTHING;
