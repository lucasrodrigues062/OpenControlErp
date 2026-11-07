# Data Model: Product Master Data, Brazilian Taxation, Multi-Warehouse Inventory & Traceability

**Feature Branch**: `008-product-inventory-catalog`  
**Date**: 2026-11-07  
**Database**: PostgreSQL 16+ via Flyway migration `V1_0_11__create_product_and_inventory_tables.sql`

---

## 1. Entity-Relationship Diagram

```mermaid
erDiagram
    PRODUCT_CATEGORIES ||--o{ PRODUCT_CATEGORIES : "subcategories"
    PRODUCT_CATEGORIES ||--o{ PRODUCTS : "classifies"
    PRODUCTS ||--o{ PRODUCT_UNIT_CONVERSIONS : "has"
    PRODUCTS ||--o{ PRODUCT_PRICES : "priced in"
    PRICE_LISTS ||--o{ PRODUCT_PRICES : "contains"
    
    COMPANY_BRANCHES ||--o{ WAREHOUSES : "operates"
    WAREHOUSES ||--o{ STORAGE_LOCATIONS : "contains"
    WAREHOUSES ||--o{ STOCK_BALANCES : "stores"
    STORAGE_LOCATIONS ||--o{ STOCK_BALANCES : "locates"
    PRODUCTS ||--o{ STOCK_BALANCES : "quantified in"
    
    PRODUCTS ||--o{ PRODUCT_LOTS : "tracked by"
    WAREHOUSES ||--o{ PRODUCT_LOTS : "holds"
    PRODUCTS ||--o{ PRODUCT_SERIAL_NUMBERS : "tracked by"
    WAREHOUSES ||--o{ PRODUCT_SERIAL_NUMBERS : "stores"
    PRODUCT_LOTS ||--o{ PRODUCT_SERIAL_NUMBERS : "originates from"
    
    PRODUCTS ||--o{ STOCK_MOVEMENTS : "transacted"
    WAREHOUSES ||--o{ STOCK_MOVEMENTS : "source/destination"
    STOCK_MOVEMENTS }o--|| PRODUCT_LOTS : "specifies"
    STOCK_MOVEMENTS }o--|| PRODUCT_SERIAL_NUMBERS : "specifies"

    PRODUCTS {
        UUID id PK
        UUID tenant_id FK
        UUID branch_id FK "nullable"
        VARCHAR sku UK
        VARCHAR barcode UK "nullable"
        VARCHAR name
        VARCHAR description "nullable"
        VARCHAR item_type "SPED 0200"
        VARCHAR unit_of_measure
        UUID category_id FK
        VARCHAR brand "nullable"
        VARCHAR model "nullable"
        VARCHAR ncm
        VARCHAR ncm_exception "nullable"
        VARCHAR cest "nullable"
        INT origin
        VARCHAR tracking_mode "NONE, BATCH_LOT, SERIAL_NUMBER"
        NUMERIC gross_weight "nullable"
        NUMERIC net_weight "nullable"
        NUMERIC width "nullable"
        NUMERIC height "nullable"
        NUMERIC depth "nullable"
        NUMERIC volume "nullable"
        NUMERIC min_stock
        NUMERIC max_stock "nullable"
        NUMERIC reorder_point "nullable"
        NUMERIC last_acquisition_cost
        NUMERIC average_cost
        NUMERIC replacement_cost
        BOOLEAN is_active
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    PRODUCT_CATEGORIES {
        UUID id PK
        UUID tenant_id FK
        UUID parent_id FK "nullable"
        VARCHAR code UK
        VARCHAR name
        VARCHAR color_hex
        BOOLEAN is_active
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    PRODUCT_UNIT_CONVERSIONS {
        UUID id PK
        UUID tenant_id FK
        UUID product_id FK
        VARCHAR unit_from
        VARCHAR unit_to
        NUMERIC conversion_factor
        TIMESTAMP created_at
    }

    WAREHOUSES {
        UUID id PK
        UUID tenant_id FK
        UUID branch_id FK
        VARCHAR code UK
        VARCHAR name
        VARCHAR warehouse_type "COMMERCIAL, STORAGE, QUARANTINE, TRANSIT"
        BOOLEAN is_default
        BOOLEAN is_active
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    STORAGE_LOCATIONS {
        UUID id PK
        UUID tenant_id FK
        UUID warehouse_id FK
        VARCHAR aisle
        VARCHAR rack
        VARCHAR shelf
        VARCHAR bin
        VARCHAR formatted_address UK
        BOOLEAN is_active
        TIMESTAMP created_at
    }

    STOCK_BALANCES {
        UUID id PK
        UUID tenant_id FK
        UUID warehouse_id FK
        UUID product_id FK
        UUID location_id FK "nullable"
        NUMERIC quantity_on_hand
        NUMERIC quantity_reserved
        NUMERIC quantity_available
        BIGINT version "Optimistic Locking"
        TIMESTAMP updated_at
    }

    PRODUCT_LOTS {
        UUID id PK
        UUID tenant_id FK
        UUID warehouse_id FK
        UUID product_id FK
        VARCHAR lot_number
        DATE manufacturing_date
        DATE expiration_date
        NUMERIC quantity_on_hand
        NUMERIC quantity_reserved
        VARCHAR status "RELEASED, QUARANTINE, BLOCKED, EXPIRED"
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    PRODUCT_SERIAL_NUMBERS {
        UUID id PK
        UUID tenant_id FK
        UUID warehouse_id FK
        UUID product_id FK
        UUID lot_id FK "nullable"
        VARCHAR serial_number UK
        VARCHAR status "IN_STOCK, RESERVED, DISPATCHED_SOLD, RMA_MAINTENANCE, WRITTEN_OFF"
        DATE warranty_until "nullable"
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    PRICE_LISTS {
        UUID id PK
        UUID tenant_id FK
        VARCHAR code UK
        VARCHAR name
        VARCHAR currency
        NUMERIC discount_or_markup_pct
        BOOLEAN is_default
        DATE valid_from "nullable"
        DATE valid_until "nullable"
        BOOLEAN is_active
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    PRODUCT_PRICES {
        UUID id PK
        UUID tenant_id FK
        UUID price_list_id FK
        UUID product_id FK
        NUMERIC sales_price
        NUMERIC promotional_price "nullable"
        DATE promo_start "nullable"
        DATE promo_end "nullable"
        TIMESTAMP updated_at
    }

    STOCK_MOVEMENTS {
        UUID id PK
        UUID tenant_id FK
        UUID branch_id FK
        VARCHAR movement_type
        UUID product_id FK
        UUID source_warehouse_id FK "nullable"
        UUID destination_warehouse_id FK "nullable"
        UUID location_id FK "nullable"
        UUID lot_id FK "nullable"
        UUID serial_number_id FK "nullable"
        NUMERIC quantity
        NUMERIC unit_cost
        NUMERIC balance_after
        VARCHAR reference_document "nullable"
        VARCHAR reason_code "nullable"
        TEXT notes "nullable"
        UUID created_by_user_id
        TIMESTAMP created_at
    }
```

---

## 2. Enumerations

### 2.1 Item Type (`SpedItemType`)
- `REV_00`: Mercadoria para Revenda
- `MP_01`: Matéria-Prima
- `EMB_02`: Embalagem
- `PP_03`: Produto em Processo
- `PA_04`: Produto Acabado
- `SP_05`: Subproduto
- `PI_06`: Produto Intermediário
- `UC_07`: Material de Uso e Consumo
- `AI_08`: Ativo Imobilizado
- `SRV_09`: Serviços
- `OUT_10`: Outros Insumos
- `OUT_99`: Outras

### 2.2 Tracking Mode (`ProductTrackingMode`)
- `NONE`: Standard non-traceable inventory
- `BATCH_LOT`: Batch/lot controlled (requires lot number, manufacturing date, expiry date, FEFO)
- `SERIAL_NUMBER`: Unit-level serial number controlled

### 2.3 Warehouse Type (`WarehouseType`)
- `COMMERCIAL`: Main sales / distribution depot
- `STORAGE`: Long-term / secondary storage warehouse
- `QUARANTINE`: Damaged, inspection, or quarantine stock
- `TRANSIT`: Temporary transit warehouse for inter-branch transfer

### 2.4 Lot Status (`LotStatus`)
- `RELEASED`: Available for commercial dispatch and picking
- `QUARANTINE`: Temporarily held for quality inspection
- `BLOCKED`: Administratively blocked from dispatch
- `EXPIRED`: Passed expiration date (blocked automatically)

### 2.5 Serial Number Status (`SerialNumberStatus`)
- `IN_STOCK`: Physically present and available
- `RESERVED`: Allocated to an open sales order
- `DISPATCHED_SOLD`: Sold and delivered to a customer
- `RMA_MAINTENANCE`: In maintenance or technical warranty evaluation
- `WRITTEN_OFF`: Disposed, discarded, or lost

### 2.6 Stock Movement Type (`StockMovementType`)
- `INBOUND_PURCHASE`: Inbound receipt from supplier invoice
- `OUTBOUND_SALE`: Outbound shipment for customer order
- `TRANSFER_OUT`: Inter-warehouse outbound deduction
- `TRANSFER_IN`: Inter-warehouse inbound addition
- `ADJUSTMENT_INVENTORY`: Manual cycle count correction
- `RETURN_CUSTOMER`: Customer returned goods
- `RETURN_SUPPLIER`: Goods returned to supplier

---

## 3. Database Indexes & Integrity Constraints

1. **Unique Constraints**:
   - `products`: `UNIQUE (tenant_id, sku)`
   - `products`: `UNIQUE (tenant_id, barcode)` WHERE `barcode IS NOT NULL`
   - `product_categories`: `UNIQUE (tenant_id, code)`
   - `warehouses`: `UNIQUE (tenant_id, code)`
   - `storage_locations`: `UNIQUE (warehouse_id, formatted_address)`
   - `stock_balances`: `UNIQUE (warehouse_id, product_id, location_id)`
   - `product_serial_numbers`: `UNIQUE (tenant_id, product_id, serial_number)`
   - `price_lists`: `UNIQUE (tenant_id, code)`
   - `product_prices`: `UNIQUE (price_list_id, product_id)`

2. **Performance Indexes**:
   - `idx_products_tenant_name`: `(tenant_id, name)`
   - `idx_products_ncm`: `(tenant_id, ncm)`
   - `idx_stock_balances_product`: `(tenant_id, product_id)`
   - `idx_lots_product_expiry`: `(product_id, expiration_date ASC)` WHERE `status = 'RELEASED'` (FEFO optimization)
   - `idx_stock_movements_product_created`: `(tenant_id, product_id, created_at DESC)` (Kardex chronological query)
