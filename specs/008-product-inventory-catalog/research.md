# Research & Technical Decisions: Product Catalog, Brazilian Taxation, Multi-Warehouse Inventory & Traceability

**Feature Branch**: `008-product-inventory-catalog`  
**Date**: 2026-11-07  
**Spec Reference**: [specs/008-product-inventory-catalog/spec.md](specs/008-product-inventory-catalog/spec.md)

---

## 1. Domain Decomposition & High-Cohesion Modules

### Context & Problem
In traditional monolithic ERPs (such as Cronos `dbo.ESTOQUE` or legacy systems), products, physical stock, price tables, fiscal codes, lots, and serial numbers are frequently merged into a single massive flat table of 150+ columns. This causes:
- Lock contention during stock updates.
- Inability to handle multiple warehouses without duplicating master data.
- Coupling between commercial catalog definitions and physical logistical positions.

### Architectural Decision
Adopt Clean Domain-Driven Design (DDD) with two highly cohesive, complementary module boundaries:
1. **Catalog Domain (`org.opencontrol.erp.catalog`)**:
   - Master data: `Product`, `ProductCategory`, `ProductUnitConversion`.
   - Commercial Pricing: `PriceList`, `ProductPrice`.
   - Brazilian Fiscal definitions: NCM, CEST, Origin, SPED 0200 types.
2. **Inventory Domain (`org.opencontrol.erp.inventory`)**:
   - Physical storage: `Warehouse`, `StorageLocation` (Bin).
   - Stock balances: `StockBalance` (segmented by Tenant, Branch, Warehouse, Product, and Bin).
   - Traceability: `ProductLot` (batch/expiry/FEFO), `ProductSerialNumber` (individual unit tracking).
   - Ledger: `StockMovement` (Kardex chronological audit trail).

Both domains share common tenant/branch boundaries and interact via Spring Modulith domain events (`ProductCreatedEvent`, `StockMovementRecordedEvent`, `LotExpiredEvent`) and direct service interfaces without cyclic dependencies.

---

## 2. Brazilian Fiscal & Regulatory Standards Compliance

### 2.1 SPED Fiscal - Tabela 0200 (Tipo do Item)
Brazilian tax authorities (Receita Federal / SEFAZ) require all transacted goods and services to carry a standardized item classification in the SPED EFD ICMS/IPI registry:
- `00` - Mercadoria para Revenda (Resale Goods)
- `01` - Matéria-Prima (Raw Materials)
- `02` - Embalagem (Packaging)
- `03` - Produto em Processo (Work in Progress)
- `04` - Produto Acabado (Finished Goods)
- `05` - Subproduto (Byproduct)
- `06` - Produto Intermediário (Intermediate Product)
- `07` - Material de Uso e Consumo (Usage & Consumption)
- `08` - Ativo Imobilizado (Fixed Asset)
- `09` - Serviços (Services)
- `10` - Outros Insumos (Other Inputs)
- `99` - Outras (Other)

### 2.2 NCM & CEST Validation
- **NCM (Nomenclatura Comum do Mercosul)**: Exactly 8 numeric digits. Formatted as `####.##.##` on UI, stored raw (8 digits) in database.
- **EX TIPI**: Optional exception code (up to 3 characters) within an NCM chapter for industrial tax exemption/differentiation.
- **CEST (Código Especificador da Substituição Tributária)**: Exactly 7 numeric digits. Required for goods subject to tax substitution (ICMS-ST). Stored raw, masked as `##.###.##`.
- **Origem da Mercadoria (SEFAZ Table)**: 1-digit code (0 to 8) indicating domestic, direct foreign import, or domestic market acquisition of imported items.

### 2.3 Barcode Validation (GTIN / EAN Check Digit Modulo 10)
Standard GS1/EAN barcodes (EAN-8, EAN-13, EAN-14) enforce Modulo 10 checksum validation:
- Alternating weights of 3 and 1 starting from the rightmost digit before the check digit.
- Prevents scanner misreads and typing typos during cataloging.

---

## 3. Inventory Valuation & Cost Accounting (CMP vs. FIFO)

### Decision: Weighted Average Cost (Custo Médio Ponderado - CMP)
Brazilian corporate tax law (RIR/Decreto nº 9.580/2018, art. 306) and CPC 16 (R1) / IAS 2 mandate that inventory valuation must follow either **Weighted Average Cost (CMP)** or **PEPS/FIFO (Primeiro que Entra, Primeiro que Sai)**. LIFO/UEPS is illegal under Brazilian tax regulations.

For OpenControl ERP:
- **Weighted Average Cost (CMP)** is the primary valuation algorithm:
  $$\text{New CMP} = \frac{(\text{Current Qty} \times \text{Current CMP}) + (\text{Inbound Qty} \times \text{Inbound Unit Cost})}{\text{Current Qty} + \text{Inbound Qty}}$$
- Outbound transactions decrement stock value at the current CMP without altering the unit CMP.
- The system also persists `lastAcquisitionCost` and `replacementCost` for commercial pricing markup calculations.

---

## 4. Multi-Warehouse & Physical Logistical Bin Addressing (WMS)

### Segregation Model
- Warehouses are partitioned by Company Branch (`branchId` FK to `company_branches`).
- Stock balances are strictly atomic per `(tenant_id, warehouse_id, product_id, location_id)`.
- Storage locations use a hierarchical addressing scheme:
  - **Aisle (Rua)**: e.g., `A`
  - **Rack (Estante / Módulo)**: e.g., `04`
  - **Shelf (Nível / Prateleira)**: e.g., `02`
  - **Bin (Box / Vão)**: e.g., `01`
  - Computed Formatted Address: `A-04-02-01`
- Optimistic locking with `@Version` on `StockBalance` avoids race conditions during parallel sales order reservations and inventory receipts.

---

## 5. Traceability: Lot/Batch Management (FEFO) & Serial Numbers

### 5.1 Batch / Lot (FEFO Rule)
Regulated goods (pharma, food, chemicals) enforce First Expire, First Out:
- Attributes: `lotNumber`, `manufacturingDate`, `expirationDate`, `status` (`RELEASED`, `QUARANTINE`, `BLOCKED`, `EXPIRED`).
- During sales reservation/picking, active lots are automatically ordered by `expirationDate ASC`.
- Expired lots (`expirationDate < CURRENT_DATE`) cannot be selected for commercial fulfillment and trigger automated status transition to `EXPIRED`.

### 5.2 Serial Number Tracking
- For individualized high-value items (computers, serialized tools, machinery):
- Each unit possesses a unique `serialNumber` scoped to `(tenant_id, product_id)`.
- Lifecycle status machine:
  `IN_STOCK` $\rightarrow$ `RESERVED` $\rightarrow$ `DISPATCHED_SOLD` $\rightarrow$ `RMA_MAINTENANCE` $\rightarrow$ `WRITTEN_OFF`.

---

## 6. Immutable Kardex Ledger Pattern

### Design Decision
All stock changes write an append-only row to `stock_movements`:
- Movement Types:
  - `INBOUND_RECEIPT` (Compra / Entrada por NF)
  - `OUTBOUND_DISPATCH` (Venda / Expedição)
  - `TRANSFER_OUT` & `TRANSFER_IN` (Transferência entre Depósitos)
  - `ADJUSTMENT_POSITIVE` & `ADJUSTMENT_NEGATIVE` (Ajuste de Inventário)
  - `RETURN_CUSTOMER` & `RETURN_SUPPLIER` (Devolução)
  - `PRODUCTION_INPUT` & `PRODUCTION_OUTPUT` (Ordem de Produção)
- Contains snapshot of `unitCost`, `quantity`, `balanceBefore`, and `balanceAfter`.
- Historical Kardex entries cannot be updated or deleted (`UPDATE`/`DELETE` blocked at application service and database trigger levels).
