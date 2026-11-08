# Data Model: Inbound Purchases, Fiscal Invoicing, Landed Costing & Inventory Transfers

**Feature**: `009-inbound-purchases-transfers`
**Date**: 2026-11-08

---

## Entity Relationship Diagram

```mermaid
erDiagram
    PURCHASE_ORDER ||--o{ PURCHASE_ORDER_ITEM : contains
    INBOUND_INVOICE ||--o{ INBOUND_INVOICE_ITEM : contains
    INBOUND_INVOICE ||--o{ LANDED_COST_ALLOCATION : allocates
    GOODS_RECEIPT ||--o{ GOODS_RECEIPT_ITEM : verifies
    STOCK_TRANSFER ||--o{ STOCK_TRANSFER_ITEM : details
    COST_FORMULA_PROFILE ||--o{ INBOUND_INVOICE : "applied to"

    PURCHASE_ORDER {
        uuid id PK
        uuid tenant_id
        uuid branch_id
        string order_number
        uuid supplier_id
        date order_date
        date expected_delivery_date
        string status
        numeric total_amount
        string currency
        text notes
    }

    PURCHASE_ORDER_ITEM {
        uuid id PK
        uuid purchase_order_id FK
        uuid product_id
        numeric quantity_ordered
        numeric quantity_received
        numeric unit_price
        numeric discount_amount
        uuid target_warehouse_id
    }

    COST_FORMULA_PROFILE {
        uuid id PK
        uuid tenant_id
        string code
        string name
        text formula_expression
        string freight_proration_method
        boolean is_default
        boolean is_active
    }

    INBOUND_INVOICE {
        uuid id PK
        uuid tenant_id
        uuid branch_id
        uuid supplier_id
        uuid cost_formula_profile_id FK
        string access_key
        string invoice_number
        string series
        date issuance_date
        date entry_date
        numeric total_products
        numeric total_freight
        numeric total_insurance
        numeric total_expenses
        numeric total_discount
        numeric total_invoice_amount
        string status
        text raw_xml_payload
    }

    INBOUND_INVOICE_ITEM {
        uuid id PK
        uuid inbound_invoice_id FK
        integer item_index
        string supplier_item_code
        string supplier_item_description
        uuid matched_product_id
        string supplier_uom
        string internal_uom
        numeric conversion_factor
        numeric invoiced_quantity
        numeric internal_quantity
        numeric unit_price
        numeric total_price
        numeric allocated_freight
        numeric allocated_insurance
        numeric allocated_expenses
        numeric icms_amount
        numeric icms_credit_amount
        numeric ipi_amount
        numeric pis_amount
        numeric pis_credit_amount
        numeric cofins_amount
        numeric cofins_credit_amount
        numeric final_landed_unit_cost
        numeric final_total_cost
    }

    GOODS_RECEIPT {
        uuid id PK
        uuid tenant_id
        uuid branch_id
        uuid warehouse_id
        uuid purchase_order_id FK
        uuid inbound_invoice_id FK
        string receipt_number
        date receipt_date
        string status
        string conference_notes
    }

    GOODS_RECEIPT_ITEM {
        uuid id PK
        uuid goods_receipt_id FK
        uuid product_id
        numeric expected_quantity
        numeric received_quantity
        numeric rejected_quantity
        string lot_number
        date fabrication_date
        date expiration_date
        text serial_numbers_json
    }

    STOCK_TRANSFER {
        uuid id PK
        uuid tenant_id
        string transfer_number
        string transfer_type
        uuid origin_branch_id
        uuid origin_warehouse_id
        uuid destination_branch_id
        uuid destination_warehouse_id
        string status
        timestamp dispatched_at
        timestamp received_at
        text tracking_notes
    }

    STOCK_TRANSFER_ITEM {
        uuid id PK
        uuid stock_transfer_id FK
        uuid product_id
        numeric requested_quantity
        numeric shipped_quantity
        numeric received_quantity
        numeric unit_cost
        uuid lot_id
        text serial_numbers_json
    }

    SUPPLIER_PRODUCT_MAPPING {
        uuid id PK
        uuid tenant_id
        uuid supplier_id
        string supplier_item_code
        string supplier_barcode
        uuid product_id
        string supplier_uom
        numeric conversion_factor
    }
```

---

## Database Schemas & Constraints

### 1. `cost_formula_profiles`
Configurable landed cost formulas (Excel-like dynamic engine).
```sql
CREATE TABLE cost_formula_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    formula_expression TEXT NOT NULL,
    freight_proration_method VARCHAR(30) NOT NULL DEFAULT 'VALUE_PROPORTIONAL', -- 'VALUE_PROPORTIONAL', 'WEIGHT_PROPORTIONAL'
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cost_profile_tenant_code UNIQUE (tenant_id, code)
);
```

### 2. `supplier_product_mappings`
De-para and conversion factor memory between vendor product codes and internal catalog SKUs.
```sql
CREATE TABLE supplier_product_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    supplier_id UUID NOT NULL,
    supplier_item_code VARCHAR(100) NOT NULL,
    supplier_barcode VARCHAR(50),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    supplier_uom VARCHAR(10) NOT NULL DEFAULT 'UN',
    conversion_factor NUMERIC(15, 4) NOT NULL DEFAULT 1.0000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_supplier_product_mapping UNIQUE (tenant_id, supplier_id, supplier_item_code)
);
```

### 3. `purchase_orders` & `purchase_order_items`
Procurement commercial commitment before goods delivery.
- Status enum: `DRAFT`, `APPROVED`, `PARTIALLY_RECEIVED`, `FULLY_RECEIVED`, `CANCELLED`.

### 4. `inbound_invoices` & `inbound_invoice_items`
NF-e documents and computed landed costs per item.
- Status enum: `DRAFT`, `PENDING_CONFERENCE`, `CONFERRED`, `STOCKED`, `CANCELLED`.

### 5. `goods_receipts` & `goods_receipt_items`
Physical receiving and blind conference records with lot and serial numbers.
- Status enum: `IN_PROGRESS`, `CONFERRED_OK`, `CONFERRED_WITH_DIVERGENCE`, `CANCELLED`.

### 6. `stock_transfers` & `stock_transfer_items`
Internal warehouse and inter-branch movements.
- Transfer Type: `INTERNAL_WAREHOUSE`, `INTER_BRANCH`.
- Status enum: `DRAFT`, `DISPATCHED_IN_TRANSIT`, `RECEIVED_COMPLETED`, `CANCELLED`.
