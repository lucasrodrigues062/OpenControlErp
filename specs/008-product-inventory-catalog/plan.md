# Implementation Plan: Product Master Data, Brazilian Taxation, Multi-Warehouse Inventory & Traceability

**Branch**: `008-product-inventory-catalog` | **Date**: 2026-11-07 | **Spec**: [specs/008-product-inventory-catalog/spec.md](specs/008-product-inventory-catalog/spec.md)

**Input**: Feature specification from `specs/008-product-inventory-catalog/spec.md` with Brazilian ERP master data compliance.

---

## Summary

Deliver the foundational Product Catalog, Pricing Tables, Multi-Warehouse Inventory, and Traceability module for OpenControl ERP MVP. The implementation separates catalog definitions from physical inventory:
1. **Catalog Domain (`org.opencontrol.erp.catalog`)**: Master item data, SPED Fiscal 0200 types, unit conversions, statutory Brazilian taxation (8-digit NCM, 7-digit CEST, Origin, Tax groups), and flexible Price Lists with promotional validity.
2. **Inventory Domain (`org.opencontrol.erp.inventory`)**: Multi-warehouse partitioning by company branch, logistical bin location addressing (Aisle-Rack-Shelf-Bin), atomic stock positions (on hand, reserved, available), and safety threshold alerts.
3. **Traceability Engine**: Automated FEFO (First Expire, First Out) batch/lot allocation with expiration controls, and individual serialized asset tracking across lifecycle stages.
4. **Immutable Kardex Ledger**: Append-only transactional history capturing unit costs (Weighted Average Cost - CMP), balance snapshots, reasons, and user audit metadata.

---

## Technical Context

**Language/Version**: Kotlin 2.2 targeting JVM 21 LTS (Backend) | TypeScript 5.8+ (Frontend)  
**Primary Dependencies**: Spring Boot 4.1.2, Spring Modulith, Spring Data JPA, Hibernate, Flyway, PostgreSQL JDBC, Jackson | React 19, Vite 6, Tailwind CSS, Lucide React, TanStack Query, Axios  
**Storage**: PostgreSQL 16+ via Flyway migration `V1_0_11__create_product_and_inventory_tables.sql`  
**Testing**: JUnit 5, MockK, AssertJ, Spring MockMvc, Testcontainers PostgreSQL | Vitest, React Testing Library  
**Target Platform**: Web application (Linux / Docker server runtime, responsive desktop browser interface)  
**Project Type**: Web Application (Backend Spring Boot API + Frontend React Vite SPA)  
**Performance Goals**:
- Product search & barcode lookup: <80ms p95 latency
- Stock balance query per warehouse: <120ms
- Stock movement posting & balance update transaction: <200ms
- Database indexing: B-tree indexes on `(tenant_id, sku)`, `(tenant_id, barcode)`, `(warehouse_id, product_id)`, and `(product_id, expiration_date)`  
**Constraints**: Zero negative stock without explicit policy override; strict validation of NCM (8 digits) and CEST (7 digits); append-only Kardex ledger.  
**Scale/Scope**: 11 relational database tables; 24 REST endpoints; 5 responsive frontend views (Products List & Form, Categories, Warehouses & Locations, Stock Balances & Movements, Lots & Serials).

---

## Constitution Check

*GATE: All principles evaluated against OpenControl ERP Constitution v1.3.0. Gate status: PASSED.*

| Constitution Principle | Evaluation & Compliance Strategy | Gate Status |
|---|---|---|
| **I. Modular Monolith & Clean Architecture** | Catalog and Inventory are implemented under clean DDD architecture. Cross-module operations (e.g., from Sales or Invoicing) communicate via published service contracts or domain events (`ProductCreatedEvent`, `StockMovementRecordedEvent`). | **PASS** |
| **II. Extensibility via Plugin & App Architecture** | Inventory movements emit lifecycle hooks allowing extensions to intercept or validate stock disbursements (e.g., quality inspection plugins). Supports dynamic Custom Fields for `PRODUCT`. | **PASS** |
| **III. Brazil-First Localization with Multi-Jurisdiction Abstraction** | Brazilian tax metadata strictly supported: NCM (8 digits), CEST (7 digits), Origin (0 to 8), SPED Fiscal Tabela 0200 item types, and Weighted Average Cost (CMP) standard. Multi-currency and metric/imperial unit abstractions preserved. | **PASS** |
| **IV. Codebase Language & Jurisdiction Nomenclature Standards** | Codebase, classes, database schemas, and REST APIs are 100% in English (`Product`, `Warehouse`, `StockBalance`, `ProductLot`, `ProductSerialNumber`, `StockMovement`). Statutory acronyms without direct English translation remain standard (`Ncm`, `Cest`). | **PASS** |
| **V. Modern Componentized UI with Design System Discipline** | Built with React, Tailwind CSS, Lucide icons, and accessible modal dialogs. Employs `data-dense-dashboard` principles for high information density, inline filters, and bilingual support (`pt-BR` and `en-US`). | **PASS** |
| **VI. Transactional Integrity, Financial Immutability & Auditability** | Kardex transactions are append-only. Stock adjustments require mandatory reason codes and emit immutable audit events to the audit service. | **PASS** |
| **VII. Minimum 90% Automated Test Coverage Gate** | Minimum 90% line and branch test coverage enforced via `jacocoTestCoverageVerification`. Unit tests cover CMP calculation, FEFO lot sorting, barcode check digits, balance deductions, and controllers. | **PASS** |
| **VIII. Mandatory Feature Documentation & Contract Transparency** | All artifacts generated: `spec.md`, `research.md`, `data-model.md`, `catalog-inventory-api.yaml` (OpenAPI 3.0), and `quickstart.md`. KDoc docstrings on all public classes and methods. | **PASS** |

---

## Project Structure

### Documentation (`specs/008-product-inventory-catalog/`)

```text
specs/008-product-inventory-catalog/
├── spec.md                     # Feature specification (User Stories 1-5, Requirements, Success Criteria)
├── plan.md                     # This implementation plan
├── research.md                 # Phase 0: SPED 0200, CMP vs FIFO, FEFO algorithm, Kardex ledger
├── data-model.md               # Phase 1: PostgreSQL ER diagram, schemas, and state transitions
├── contracts/
│   └── catalog-inventory-api.yaml # Phase 1: OpenAPI 3.0 REST specification
├── quickstart.md               # Phase 1: Runnable end-to-end verification scenarios
└── checklists/
    └── requirements.md         # Requirements traceability checklist
```

### Source Code (`backend/` & `frontend/`)

```text
backend/
├── src/main/kotlin/org/opencontrol/erp/
│   ├── catalog/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Product.kt
│   │   │   │   ├── ProductCategory.kt
│   │   │   │   ├── ProductUnitConversion.kt
│   │   │   │   ├── PriceList.kt
│   │   │   │   ├── ProductPrice.kt
│   │   │   │   └── CatalogEnums.kt
│   │   │   ├── validation/
│   │   │   │   └── BarcodeValidator.kt
│   │   │   └── events/
│   │   │       └── CatalogDomainEvents.kt
│   │   ├── repository/
│   │   │   ├── ProductRepository.kt
│   │   │   ├── ProductCategoryRepository.kt
│   │   │   └── PriceListRepository.kt
│   │   ├── service/
│   │   │   ├── ProductService.kt
│   │   │   ├── ProductCategoryService.kt
│   │   │   └── PricingService.kt
│   │   └── api/
│   │       ├── ProductController.kt
│   │       ├── ProductCategoryController.kt
│   │       └── dto/
│   │           ├── ProductDtos.kt
│   │           └── CategoryDtos.kt
│   └── inventory/
│       ├── domain/
│       │   ├── model/
│       │   │   ├── Warehouse.kt
│       │   │   ├── StorageLocation.kt
│       │   │   ├── StockBalance.kt
│       │   │   ├── ProductLot.kt
│       │   │   ├── ProductSerialNumber.kt
│       │   │   ├── StockMovement.kt
│       │   │   └── InventoryEnums.kt
│       │   └── events/
│       │       └── InventoryDomainEvents.kt
│       ├── repository/
│       │   ├── WarehouseRepository.kt
│       │   ├── StorageLocationRepository.kt
│       │   ├── StockBalanceRepository.kt
│       │   ├── ProductLotRepository.kt
│       │   ├── ProductSerialNumberRepository.kt
│       │   └── StockMovementRepository.kt
│       ├── service/
│       │   ├── WarehouseService.kt
│       │   ├── StockBalanceService.kt
│       │   ├── TraceabilityService.kt
│       │   └── KardexMovementService.kt
│       └── api/
│           ├── WarehouseController.kt
│           ├── StockBalanceController.kt
│           ├── TraceabilityController.kt
│           └── dto/
│               ├── WarehouseDtos.kt
│               ├── StockBalanceDtos.kt
│               └── TraceabilityDtos.kt
├── src/main/resources/db/migration/
│   └── V1_0_11__create_product_and_inventory_tables.sql
└── src/test/kotlin/org/opencontrol/erp/
    ├── catalog/
    └── inventory/

frontend/
├── src/
│   ├── types/
│   │   ├── catalog.ts
│   │   └── inventory.ts
│   ├── services/
│   │   ├── catalogApi.ts
│   │   └── inventoryApi.ts
│   ├── pages/
│   │   ├── catalog/
│   │   │   ├── ProductsPage.tsx
│   │   │   ├── ProductDetailPage.tsx
│   │   │   └── CategoriesPage.tsx
│   │   └── inventory/
│   │       ├── WarehousesPage.tsx
│   │       ├── StockBalancesPage.tsx
│   │       ├── LotsAndSerialsPage.tsx
│   │       └── KardexMovementsPage.tsx
│   ├── components/
│   │   ├── catalog/
│   │   └── inventory/
│   └── locales/
│       ├── pt-BR/
│       │   ├── catalog.ts
│       │   └── inventory.ts
│       └── en-US/
│           ├── catalog.ts
│           └── inventory.ts
```

---

## Rollout & Execution Phases

- **Phase 1: Catalog & Brazilian Tax Core (User Story 1)**: Database tables, master product models, category tree, EAN-13 validation, NCM/CEST constraints, and catalog UI.
- **Phase 2: Multi-Warehouse & Bin Addressing (User Story 2)**: Warehouse partitions by branch, logistical addresses, stock balance aggregation per location.
- **Phase 3: Traceability - Lots (FEFO) & Serials (User Story 3)**: Batch tracking with expiration check, FEFO suggestion, unique serial number lifecycle.
- **Phase 4: Pricing Tables & Cost Accounting (User Story 4)**: Weighted average cost (CMP) recalculation, pricing tables with promotional scheduling.
- **Phase 5: Kardex Movements & Ledger Verification (User Story 5)**: Immutable stock ledger, manual adjustments with reason codes, transfer confirmation, end-to-end auditability.
