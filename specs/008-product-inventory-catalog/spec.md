# Feature Specification: Product Master Data, Tax Rules, Multi-Warehouse Inventory, Lots & Serial Numbers

**Feature Branch**: `008-product-inventory-catalog`

**Created**: 2026-11-07

**Status**: Draft

**Input**: User description: "Agora vamos criar todo o cadastro referente a produtos, categoria, tributação, preço, estoque, depositos, Endereço/local de armazenamento, controle de lote, numero de serie, e algo mais que eu possa ter esquecido"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Product Master Data, Categorization, Units & Brazilian Fiscal Compliance (Priority: P1) 🎯 MVP

As an Inventory Manager or Catalog Operator,  
I want to register and organize commercial and industrial items with standardized SKUs, barcodes (EAN/GTIN), hierarchical categories, units of measure with conversion factors, and statutory Brazilian fiscal parameters (NCM, CEST, Origin, SPED item type),  
So that the enterprise maintains an accurate, centralized catalog ready for commercial pricing, inventory management, and fiscal invoicing (NF-e/NFC-e).

**Why this priority**: Products and services are the core transactional commodity of an ERP. Master data is a strict prerequisite for inventory balances, pricing tables, purchasing, sales quotes, and tax calculation.

**Independent Test**: Register products across different SPED categories (e.g., Mercadoria para Revenda, Matéria-Prima, Produto Acabado, Serviços), assign category hierarchy, validate barcode uniqueness, assign valid 8-digit NCM, 7-digit CEST, and unit conversions (e.g., 1 Box = 12 Units), and verify catalog search and status filters.

**Acceptance Scenarios**:

1. **Given** an authenticated inventory manager, **When** they register a physical product with unique SKU, internal barcode (EAN-13), commercial name, primary category, unit of measure ("UN"), and valid NCM code, **Then** the product is saved in active status and appears in product searches.
2. **Given** a user registering a product with an invalid NCM format (not 8 numeric digits) or invalid CEST (not 7 numeric digits), **When** they submit the form, **Then** the system rejects registration with an explicit validation error.
3. **Given** a product purchased in bulk packaging (e.g., "CX" with 24 units) and sold individually ("UN"), **When** a unit conversion factor is defined, **Then** the catalog records both units and correctly computes conversions for purchasing and stock handling.
4. **Given** an existing SKU or EAN within the same tenant, **When** another product attempts to register with the identical code, **Then** the system prevents duplicate registration and informs the user.

---

### User Story 2 - Multi-Warehouse Management & Physical Bin Addressing (Priority: P2)

As a Warehouse Coordinator or Logistical Operator,  
I want to manage multiple storage warehouses (Depósitos) partitioned by branch (Filial), and configure physical logistical addresses (Rua/Aisle, Prateleira/Rack, Nível/Shelf, Box/Vão),  
So that goods are stored systematically, stock counts are segregated by physical location, and picking/replenishment routes are optimized.

**Why this priority**: Enterprise businesses operate with segregated storage (e.g., Central Distribution Center, Store Front, Damaged/Quarantine Depot, Raw Material Storage). Without multi-warehouse partitioning and location addressing, physical picking errors and phantom stock discrepancies occur.

**Independent Test**: Create multiple warehouses linked to company branches, configure warehouse zones/aisles/shelves, assign a product's primary and secondary bin locations, and verify stock balance separation per warehouse.

**Acceptance Scenarios**:

1. **Given** a company branch, **When** an operator creates warehouses (e.g., "Almoxarifado Principal" and "Depósito de Avarias"), **Then** both warehouses are available for stock allocations and transfers.
2. **Given** a warehouse, **When** the logistics manager defines physical bin addresses (e.g., Aisle A, Rack 02, Shelf 03, Bin 01 -> "A-02-03-01"), **Then** items stored in this warehouse can be bound to specific address locations.
3. **Given** a product query, **When** checking availability, **Then** the system presents total stock broken down by warehouse and specific bin address.

---

### User Story 3 - Traceability: Batch/Lot Control (FEFO) & Serial Number Tracking (Priority: P3)

As a Quality Control Inspector, Pharmacist, or Electronics Stock Manager,  
I want to enforce lot/batch control with manufacturing/expiration dates or unit-level serial number tracking on designated products,  
So that perishable/regulated goods are consumed according to expiration dates (First Expire, First Out - FEFO) and serialized assets (electronics, machinery) have full individual warranty and lifecycle tracking.

**Why this priority**: Critical statutory compliance for pharmaceuticals, food, cosmetics, chemicals (Anvisa/MAPA), and high-value serialized equipment (warranty, serial tracking, return management).

**Independent Test**: Register a lot-controlled item, receive stock specifying lot identifier, manufacturing date, and expiration date; verify FEFO dispatch ordering; register a serialized item and verify that each unit possesses a unique serial number in stock.

**Acceptance Scenarios**:

1. **Given** a product configured with `Tracking Mode = LOT`, **When** inbound stock is received without lot number or expiration date, **Then** the system blocks stock entry until lot metadata is supplied.
2. **Given** multiple active lots for the same product, **When** fulfilling stock for sales, **Then** the system suggests/allocates the lot with the nearest expiration date (FEFO rule) and warns if a lot is expired or in quarantine.
3. **Given** a product configured with `Tracking Mode = SERIAL_NUMBER`, **When** stock entry is recorded, **Then** the user must supply exact individual serial numbers matching the movement quantity, and each serial number must be globally unique for that product.
4. **Given** a serial number inquiry, **When** looking up a serial number, **Then** the system displays its current location, warehouse, status (In Stock, Sold, Under RMA/Maintenance), and historical movement chain.

---

### User Story 4 - Multi-Price Lists, Markup Calculation & Cost Accounting (Priority: P4)

As a Commercial Manager or Financial Controller,  
I want to establish acquisition costs, calculated weighted average costs (CMP), markup percentages, and manage multiple pricing tables (e.g., Varejo, Atacado, Consumidor Final) with promotional validity schedules,  
So that sales quotes apply appropriate price rules automatically and profit margins are safeguarded.

**Why this priority**: Pricing tables allow targeted commercial segmentation without creating duplicate products, while cost tracking ensures accurate profit margin calculation.

**Independent Test**: Create two price lists (Tabela Varejo and Tabela Atacado), configure a base product cost, apply a target markup percentage to suggest sales prices, set a promotional price with start/end date, and verify price resolution rules.

**Acceptance Scenarios**:

1. **Given** a product with recorded acquisition cost, **When** the manager specifies a target markup (e.g., 50%), **Then** the system calculates the suggested sales price considering tax and cost bases.
2. **Given** multiple pricing tables, **When** a customer associated with "Tabela Atacado" requests a quote, **Then** the system resolves the unit price defined in the wholesale list.
3. **Given** a promotional price scheduled between Date A and Date B, **When** an inquiry occurs within that interval, **Then** the promotional price is applied; outside the window, the standard list price returns automatically.

---

### User Story 5 - Inventory Transactions, Stock Balance & Kardex Audit Trail (Priority: P5)

As an Inventory Controller or Auditor,  
I want an immutable chronological log (Kardex) of all stock movements (inbound receipts, outbound shipments, manual adjustments, inter-warehouse transfers), capturing movement type, quantity, unit cost, source/destination warehouse, and responsible user,  
So that stock balances are 100% auditable and inventory discrepancies can be reconciled.

**Why this priority**: Guarantees accounting and physical stock reconciliation, prevents inventory theft or unaccounted loss, and satisfies fiscal inventory audit requirements (SPED Bloco K / Bloco H).

**Independent Test**: Execute an inbound receipt, a manual adjustment (with justification reason), and an inter-warehouse transfer, then verify that product balance and Kardex transaction history reflect all events with accurate running balance and unit costs.

**Acceptance Scenarios**:

1. **Given** an initial stock balance, **When** an inbound or outbound transaction occurs, **Then** the current balance (`on_hand`) updates atomically and a Kardex record is created.
2. **Given** an inventory cycle count identifying a physical discrepancy, **When** a manual stock adjustment is posted, **Then** the operator must provide an adjustment reason code (e.g., Inventory Count Discrepancy, Breakage, Expiration Disposal) and notes.
3. **Given** an inter-warehouse transfer between Warehouse A and Warehouse B, **When** confirmed, **Then** Warehouse A stock decrements, Warehouse B stock increments, and the audit trail binds both legs of the transfer.

---

### Edge Cases

- **Negative Stock Prevention**: Attempting to disburse more stock than available (`available_stock < requested_quantity`) must be blocked by default unless tenant policy explicitly permits negative stock.
- **Expired Lot Dispatch**: Attempting to select an expired lot during sales order picking must produce a blocking error or require explicit supervisor override.
- **Concurrent Stock Deduction**: When two sales orders attempt to reserve the last remaining unit of an item simultaneously, optimistic or pessimistic locking must ensure only one reservation succeeds.
- **NCM Alteration on Transacted Items**: Changing an NCM code on a product that already has historical tax notes must preserve historical records while applying the new NCM to future transactions.
- **Deleting Products with Inventory or Movements**: Products with existing stock balances or historical Kardex movements cannot be deleted; they must be deactivated (`isActive = false`) to maintain historical integrity.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Product Catalog & Taxonomy
- **FR-001**: System MUST support product master data registration including internal SKU, commercial name, trade/technical description, brand, manufacturer, model, and active/inactive status.
- **FR-002**: System MUST validate barcode uniqueness (EAN-8, EAN-13, EAN-14/DUN-14, GS1 or internal barcode) within the tenant.
- **FR-003**: System MUST support hierarchical product categories (Category and Subcategory / Family) with color codes and default tax profiles.
- **FR-004**: System MUST allow defining primary commercial unit of measure (e.g., UN, KG, LT, MT, CX, PC) and secondary/purchasing units with numeric conversion factors (e.g., 1 CX = 12 UN).
- **FR-005**: System MUST record physical dimensions (height, width, depth/length, gross weight, net weight, cubic volume) for logistics and freight calculation.

#### Brazilian Fiscal & Tax Master Data
- **FR-006**: System MUST record statutory Brazilian fiscal parameters per product: NCM code (8 digits), NCM Exception (EX TIPI), CEST code (7 digits), and Product Origin (0 to 8 according to official table).
- **FR-007**: System MUST classify items according to SPED Fiscal table 0200 item types (e.g., 00 - Mercadoria para Revenda, 01 - Matéria-Prima, 02 - Embalagem, 03 - Produto em Processo, 04 - Produto Acabado, 07 - Uso e Consumo, 09 - Serviços).
- **FR-008**: System MUST associate products with default tax rule profiles for ICMS, IPI, PIS, COFINS, and standard CSOSN/CST codes.

#### Multi-Warehouse & Bin Addressing
- **FR-009**: System MUST allow creating multiple warehouses (Depósitos) partitioned by company branch (Filial), with type designation (e.g., Comercial, Almoxarifado, Avarias, Trânsito).
- **FR-010**: System MUST support physical bin location addressing within warehouses, structured by Aisle/Rua, Rack/Estante, Shelf/Nível, and Box/Vão.
- **FR-011**: System MUST maintain segregated inventory balances per product and per warehouse: on hand (`saldo físico`), reserved (`saldo reservado`), and available (`saldo disponível`).
- **FR-012**: System MUST support threshold alerts: minimum stock (`estoque mínimo`), reorder point (`ponto de pedido`), and maximum stock (`estoque máximo`).

#### Traceability (Lots & Serial Numbers)
- **FR-013**: System MUST support configuring product traceability mode: `NONE`, `BATCH_LOT`, or `SERIAL_NUMBER`.
- **FR-014**: For lot-controlled products, the system MUST enforce lot identifier, manufacturing date, expiration date, and lot status (`RELEASED`, `QUARANTINE`, `BLOCKED`, `EXPIRED`).
- **FR-015**: System MUST enforce First Expire, First Out (FEFO) prioritization during lot picking and prevent allocating expired lots.
- **FR-016**: For serialized products, the system MUST track individual unique serial numbers, recording warranty expiration date and current asset state (`IN_STOCK`, `RESERVED`, `DISPATCHED_SOLD`, `RMA_DEFECTIVE`, `WRITTEN_OFF`).

#### Pricing & Cost Management
- **FR-017**: System MUST maintain product cost baselines: last purchase cost (`custo de aquisição`), replacement cost (`custo de reposição`), and weighted average cost (`custo médio ponderado - CMP`).
- **FR-018**: System MUST support multiple price lists (Tabelas de Preço) with name, percentage adjustment, currency, and assignment to customer groups or branches.
- **FR-019**: System MUST support scheduled promotional pricing with start and end dates.

#### Stock Movements & Kardex Audit
- **FR-020**: System MUST record all inventory changes in an immutable Kardex ledger capturing timestamp, movement type (Inbound Receipt, Outbound Dispatch, Transfer, Manual Adjustment), quantity, unit cost, running balance, source warehouse, destination warehouse, and actor user ID.
- **FR-021**: System MUST support inter-warehouse transfers with departure and arrival confirmation.
- **FR-022**: System MUST require a standardized reason code and explanatory justification for all manual inventory adjustments.

---

### Key Entities

- **Product**: Core catalog item. Attributes: `id`, `tenantId`, `branchId`, `sku`, `barcode`, `name`, `description`, `itemType` (SPED), `unitOfMeasure`, `categoryId`, `brand`, `ncm`, `cest`, `origin`, `trackingMode` (`NONE`, `BATCH_LOT`, `SERIAL_NUMBER`), `dimensions`, `weights`, `minStock`, `maxStock`, `reorderPoint`, `isActive`.
- **ProductCategory**: Hierarchical taxonomy. Attributes: `id`, `tenantId`, `parentId`, `name`, `code`, `colorHex`, `isActive`.
- **ProductUnitConversion**: Conversion between packaging units. Attributes: `id`, `productId`, `unitFrom`, `unitTo`, `conversionFactor`.
- **Warehouse**: Physical or virtual storage location. Attributes: `id`, `tenantId`, `branchId`, `code`, `name`, `warehouseType`, `isActive`.
- **StorageLocation (Bin)**: Detailed logistical address. Attributes: `id`, `warehouseId`, `aisle`, `rack`, `shelf`, `bin`, `formattedAddress`, `isActive`.
- **StockBalance**: Inventory position. Attributes: `id`, `tenantId`, `warehouseId`, `productId`, `locationId`, `quantityOnHand`, `quantityReserved`, `quantityAvailable`.
- **ProductLot (Batch)**: Lot tracking partition. Attributes: `id`, `tenantId`, `warehouseId`, `productId`, `lotNumber`, `manufacturingDate`, `expirationDate`, `quantityOnHand`, `quantityReserved`, `status`.
- **ProductSerialNumber**: Unit tracking identifier. Attributes: `id`, `tenantId`, `warehouseId`, `productId`, `serialNumber`, `lotId`, `status`, `warrantyUntil`, `currentCustomerLocation`.
- **PriceList**: Commercial price table. Attributes: `id`, `tenantId`, `code`, `name`, `isDefault`, `discountOrMarkupPct`, `validFrom`, `validUntil`, `isActive`.
- **ProductPrice**: Specific price per product per list. Attributes: `id`, `priceListId`, `productId`, `salesPrice`, `promotionalPrice`, `promoStart`, `promoEnd`.
- **StockMovement (Kardex)**: Immutable inventory ledger. Attributes: `id`, `tenantId`, `branchId`, `movementType`, `productId`, `warehouseId`, `locationId`, `lotId`, `serialNumberId`, `quantity`, `unitCost`, `balanceAfter`, `referenceDocument`, `reasonCode`, `notes`, `createdByUserId`, `createdAt`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Operators can complete a full product registration (including fiscal NCM/CEST, categorization, unit of measure, and default price) in under 60 seconds.
- **SC-002**: Stock balance queries (on hand, reserved, available) per product and across multiple warehouses return in under 150 milliseconds.
- **SC-003**: 100% of products configured for Lot or Serial tracking enforce mandatory traceability attributes upon stock receipt and dispatch without data omissions.
- **SC-004**: 100% of stock movements generate an immutable Kardex record ensuring zero discrepancy between ledger sums and physical stock position.
- **SC-005**: 100% of fiscal NCM (8 digits) and CEST (7 digits) formats are strictly validated prior to persistence, preventing fiscal invoice generation rejections.

---

## Assumptions

- **Tax Calculation Engine Integration**: This feature defines product-level statutory tax metadata (NCM, CEST, Origin, Tax Rule Groups); dynamic tax rate calculation (ICMS/PIS/COFINS calculation based on state destination CFOP) will consume this metadata during sales order quotation and invoice issuance.
- **Cost Valuation Standard**: Inventory valuation follows the Weighted Average Cost (Custo Médio Ponderado - CMP) method, standard for Brazilian statutory compliance (RIR/Decreto 9.580/2018).
- **Physical Barcode Scanning**: Web interface inputs support standard USB/Bluetooth barcode scanners (acting as keyboard wedge inputs) for fast product, lot, and serial number entry.
- **Multi-Tenancy & Branch Scoping**: Products and Price Lists can be tenant-global or branch-specific; Warehouses and Stock Balances are strictly scoped to specific Company Branches.
