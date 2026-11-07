# Tasks: Product Master Data, Tax Rules, Multi-Warehouse Inventory, Lots & Serial Numbers

**Input**: Design documents from `specs/008-product-inventory-catalog/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/catalog-inventory-api.yaml`, `quickstart.md`)

**Prerequisites**: OpenControl ERP Constitution v1.3.0, Spring Boot 4.1.2 / Kotlin 2.2 / PostgreSQL, React 19 / Vite 6

**Tests**: Mandated by Constitution Principle VII ($\ge 90\%$ test coverage on new services and APIs). Unit and integration tests are included in each user story phase.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [X] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`, `[US4]`, `[US5]`)
- Exact file paths are specified for every task

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Module directory initialization and project structure preparation

- [X] T001 Configure backend module package structure for catalog and inventory under `backend/src/main/kotlin/org/opencontrol/erp/catalog/` and `backend/src/main/kotlin/org/opencontrol/erp/inventory/`
- [X] T002 [P] Configure frontend directory structure for catalog and inventory views and components in `frontend/src/pages/catalog/`, `frontend/src/pages/inventory/`, `frontend/src/components/catalog/`, and `frontend/src/components/inventory/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core database schema migration and domain enumerations that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Create Flyway migration script `backend/src/main/resources/db/migration/V1_0_11__create_product_and_inventory_tables.sql` defining 11 tables (`product_categories`, `products`, `product_unit_conversions`, `warehouses`, `storage_locations`, `stock_balances`, `product_lots`, `product_serial_numbers`, `price_lists`, `product_prices`, `stock_movements`), foreign keys, check constraints, and B-Tree indexes
- [X] T004 [P] Implement catalog domain enumerations in `backend/src/main/kotlin/org/opencontrol/erp/catalog/domain/model/CatalogEnums.kt`: `SpedItemType` (`REV_00`, `MP_01`, `EMB_02`, `PP_03`, `PA_04`, `SP_05`, `PI_06`, `UC_07`, `AI_08`, `SRV_09`, `OUT_10`, `OUT_99`) and `ProductTrackingMode` (`NONE`, `BATCH_LOT`, `SERIAL_NUMBER`)
- [X] T005 [P] Implement inventory domain enumerations in `backend/src/main/kotlin/org/opencontrol/erp/inventory/domain/model/InventoryEnums.kt`: `WarehouseType` (`COMMERCIAL`, `STORAGE`, `QUARANTINE`, `TRANSIT`), `LotStatus` (`RELEASED`, `QUARANTINE`, `BLOCKED`, `EXPIRED`), `SerialNumberStatus` (`IN_STOCK`, `RESERVED`, `DISPATCHED_SOLD`, `RMA_MAINTENANCE`, `WRITTEN_OFF`), and `StockMovementType` (`INBOUND_PURCHASE`, `OUTBOUND_SALE`, `TRANSFER_OUT`, `TRANSFER_IN`, `ADJUSTMENT_INVENTORY`, `RETURN_CUSTOMER`, `RETURN_SUPPLIER`)
- [X] T006 [P] Implement GS1 Modulo 10 check digit barcode validator in `backend/src/main/kotlin/org/opencontrol/erp/catalog/domain/validation/BarcodeValidator.kt` supporting EAN-8, EAN-13, EAN-14 and internal codes

**Checkpoint**: Foundation ready — user story implementation can now begin.

---

## Phase 3: User Story 1 - Product Master Data, Categorization, Units & Brazilian Fiscal Compliance (Priority: P1) 🎯 MVP

**Goal**: Register and organize commercial and industrial items with standardized SKUs, barcodes (EAN/GTIN), hierarchical categories, units of measure with conversion factors, and statutory Brazilian fiscal parameters (8-digit NCM, 7-digit CEST, Origin 0-8, SPED item type).

**Independent Test**: Register products across different SPED categories, assign category hierarchy, validate barcode uniqueness, assign valid 8-digit NCM, 7-digit CEST, and unit conversions (e.g. 1 CX = 12 UN), and verify catalog search and status filters.

### Tests for User Story 1

- [X] T007 [P] [US1] Create unit tests for BarcodeValidator (Modulo 10 check digit) and fiscal format rules in `backend/src/test/kotlin/org/opencontrol/erp/catalog/domain/validation/BarcodeValidatorTest.kt`
- [X] T008 [P] [US1] Create unit and integration tests for ProductService and ProductController in `backend/src/test/kotlin/org/opencontrol/erp/catalog/service/ProductServiceTest.kt` and `backend/src/test/kotlin/org/opencontrol/erp/catalog/api/ProductControllerTest.kt`

### Implementation for User Story 1

- [X] T009 [P] [US1] Create JPA entity `ProductCategory` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/domain/model/ProductCategory.kt` with fields `id: UUID`, `tenantId: UUID`, `parentId: UUID?`, `code: String` (unique per tenant), `name: String`, `colorHex: String`, `isActive: Boolean`, `createdAt: Instant`, `updatedAt: Instant`
- [X] T010 [P] [US1] Create JPA entity `Product` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/domain/model/Product.kt` with fields `id: UUID`, `tenantId: UUID`, `branchId: UUID?`, `sku: String` (unique per tenant), `barcode: String?` (unique per tenant), `name: String`, `description: String?`, `itemType: SpedItemType`, `unitOfMeasure: String`, `categoryId: UUID`, `brand: String?`, `model: String?`, `ncm: String` (exact 8 digits), `ncmException: String?`, `cest: String?` (exact 7 digits), `origin: Int` (0 to 8), `trackingMode: ProductTrackingMode`, dimensions, weights, stock thresholds (`minStock`, `maxStock`, `reorderPoint`), cost baselines, `isActive: Boolean`
- [X] T011 [P] [US1] Create JPA entity `ProductUnitConversion` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/domain/model/ProductUnitConversion.kt` with fields `id: UUID`, `tenantId: UUID`, `productId: UUID`, `unitFrom: String`, `unitTo: String`, `conversionFactor: BigDecimal`, `createdAt: Instant`
- [X] T012 [P] [US1] Create Spring Data JPA repositories `ProductRepository`, `ProductCategoryRepository`, and `ProductUnitConversionRepository` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/repository/ProductRepository.kt` and `backend/src/main/kotlin/org/opencontrol/erp/catalog/repository/ProductCategoryRepository.kt`
- [X] T013 [P] [US1] Implement Catalog DTOs in `backend/src/main/kotlin/org/opencontrol/erp/catalog/api/dto/CatalogDtos.kt` (`CreateProductRequest`, `UpdateProductRequest`, `ProductResponse`, `ProductPageResponse`, `CreateProductCategoryRequest`, `UpdateProductCategoryRequest`, `ProductCategoryResponse`, `UnitConversionRequest`, `UnitConversionResponse`)
- [X] T014 [US1] Implement `ProductCategoryService` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/service/ProductCategoryService.kt` for category tree hierarchy, parent-child validation, and code uniqueness
- [X] T015 [US1] Implement `ProductService` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/service/ProductService.kt` with SKU/barcode deduplication, 8-digit NCM and 7-digit CEST format verification, unit conversion registration, and paginated search filtering
- [X] T016 [P] [US1] Implement `ProductCategoryController` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/api/ProductCategoryController.kt` (`GET /api/v1/catalog/categories`, `POST /api/v1/catalog/categories`, `GET /api/v1/catalog/categories/{id}`, `PUT /api/v1/catalog/categories/{id}`, `DELETE /api/v1/catalog/categories/{id}`)
- [X] T017 [US1] Implement `ProductController` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/api/ProductController.kt` (`GET /api/v1/catalog/products`, `POST /api/v1/catalog/products`, `GET /api/v1/catalog/products/{id}`, `PUT /api/v1/catalog/products/{id}`, `DELETE /api/v1/catalog/products/{id}`, `GET /api/v1/catalog/products/by-barcode/{barcode}`, `POST /api/v1/catalog/products/{id}/unit-conversions`)
- [X] T018 [P] [US1] Implement TypeScript interfaces and Axios client in `frontend/src/types/catalog.ts` and `frontend/src/services/catalogApi.ts`
- [X] T019 [P] [US1] Implement Category management page in `frontend/src/pages/catalog/CategoriesPage.tsx` with hierarchical tree navigation, badge color preview, and category modal
- [X] T020 [US1] Implement Product list and search page in `frontend/src/pages/catalog/ProductsPage.tsx` with barcode scanner search, SPED category chips, NCM filters, and pagination
- [X] T021 [US1] Implement Product registration modal/view in `frontend/src/components/catalog/ProductFormModal.tsx` with tabs for General Details, Fiscal Taxation (NCM, CEST, Origin, SPED 0200), Dimensions/Weights, and Packaging Conversions

**Checkpoint**: User Story 1 is fully functional and testable independently (MVP catalog achieved).

---

## Phase 4: User Story 2 - Multi-Warehouse Management & Physical Bin Addressing (Priority: P2)

**Goal**: Manage multiple storage warehouses partitioned by company branch, and configure physical logistical addresses (Aisle, Rack, Shelf, Bin) with formatted address strings.

**Independent Test**: Create warehouses linked to company branches, configure warehouse zones/aisles/shelves, assign a product's primary and secondary bin locations, and verify stock balance separation per warehouse.

### Tests for User Story 2

- [X] T022 [P] [US2] Create unit tests for WarehouseService in `backend/src/test/kotlin/org/opencontrol/erp/inventory/service/WarehouseServiceTest.kt`

### Implementation for User Story 2

- [X] T023 [P] [US2] Create JPA entity `Warehouse` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/domain/model/Warehouse.kt` with fields `id: UUID`, `tenantId: UUID`, `branchId: UUID`, `code: String` (unique per tenant), `name: String`, `warehouseType: WarehouseType`, `isDefault: Boolean`, `isActive: Boolean`, `createdAt: Instant`, `updatedAt: Instant`
- [X] T024 [P] [US2] Create JPA entity `StorageLocation` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/domain/model/StorageLocation.kt` with fields `id: UUID`, `tenantId: UUID`, `warehouseId: UUID`, `aisle: String`, `rack: String`, `shelf: String`, `bin: String`, `formattedAddress: String` (unique per warehouse), `isActive: Boolean`, `createdAt: Instant`
- [X] T025 [P] [US2] Create Spring Data JPA repositories `WarehouseRepository` and `StorageLocationRepository` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/repository/WarehouseRepository.kt` and `backend/src/main/kotlin/org/opencontrol/erp/inventory/repository/StorageLocationRepository.kt`
- [X] T026 [P] [US2] Create Warehouse and Location DTOs in `backend/src/main/kotlin/org/opencontrol/erp/inventory/api/dto/WarehouseDtos.kt` (`CreateWarehouseRequest`, `UpdateWarehouseRequest`, `WarehouseResponse`, `CreateStorageLocationRequest`, `StorageLocationResponse`)
- [X] T027 [US2] Implement `WarehouseService` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/service/WarehouseService.kt` for warehouse CRUD, branch scoping, default warehouse enforcement, and storage location formatting/validation
- [X] T028 [US2] Implement `WarehouseController` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/api/WarehouseController.kt` (`GET /api/v1/inventory/warehouses`, `POST /api/v1/inventory/warehouses`, `GET /api/v1/inventory/warehouses/{id}`, `PUT /api/v1/inventory/warehouses/{id}`, `GET /api/v1/inventory/warehouses/{id}/locations`, `POST /api/v1/inventory/warehouses/{id}/locations`)
- [X] T029 [P] [US2] Implement TypeScript types and API client in `frontend/src/types/inventory.ts` and `frontend/src/services/inventoryApi.ts` for warehouses and bin locations
- [X] T030 [US2] Implement Warehouse and Location management UI in `frontend/src/pages/inventory/WarehousesPage.tsx` with branch filtering, warehouse cards, and bin address configuration drawer

**Checkpoint**: User Stories 1 AND 2 work independently.

---

## Phase 5: User Story 3 - Traceability: Batch/Lot Control (FEFO) & Serial Number Tracking (Priority: P3)

**Goal**: Enforce lot/batch tracking with manufacturing and expiration dates (FEFO dispatch), and unit-level unique serial number tracking across asset lifecycle stages.

**Independent Test**: Register a lot-controlled item, receive stock specifying lot identifier, manufacturing date, and expiration date; verify FEFO dispatch ordering; register a serialized item and verify that each unit possesses a unique serial number in stock.

### Tests for User Story 3

- [X] T031 [P] [US3] Create unit tests for TraceabilityService (FEFO expiration sorting, expired lot blocking, serial number state transitions) in `backend/src/test/kotlin/org/opencontrol/erp/inventory/service/TraceabilityServiceTest.kt`

### Implementation for User Story 3

- [X] T032 [P] [US3] Create JPA entity `ProductLot` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/domain/model/ProductLot.kt` with fields `id: UUID`, `tenantId: UUID`, `warehouseId: UUID`, `productId: UUID`, `lotNumber: String`, `manufacturingDate: LocalDate`, `expirationDate: LocalDate`, `quantityOnHand: BigDecimal`, `quantityReserved: BigDecimal`, `status: LotStatus`, `createdAt: Instant`, `updatedAt: Instant`
- [X] T033 [P] [US3] Create JPA entity `ProductSerialNumber` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/domain/model/ProductSerialNumber.kt` with fields `id: UUID`, `tenantId: UUID`, `warehouseId: UUID`, `productId: UUID`, `lotId: UUID?`, `serialNumber: String` (unique per tenant & product), `status: SerialNumberStatus`, `warrantyUntil: LocalDate?`, `createdAt: Instant`, `updatedAt: Instant`
- [X] T034 [P] [US3] Create Spring Data JPA repositories `ProductLotRepository` and `ProductSerialNumberRepository` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/repository/ProductLotRepository.kt` and `backend/src/main/kotlin/org/opencontrol/erp/inventory/repository/ProductSerialNumberRepository.kt`
- [X] T035 [P] [US3] Create Traceability DTOs in `backend/src/main/kotlin/org/opencontrol/erp/inventory/api/dto/TraceabilityDtos.kt` (`CreateLotRequest`, `ProductLotResponse`, `CreateSerialNumberRequest`, `ProductSerialNumberResponse`, `UpdateSerialStatusRequest`)
- [X] T036 [US3] Implement `TraceabilityService` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/service/TraceabilityService.kt` with lot registration, FEFO allocation algorithm (`expirationDate ASC` with `status = RELEASED` and unexpired), serial number creation and lifecycle state transitions
- [X] T037 [US3] Implement `TraceabilityController` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/api/TraceabilityController.kt` (`GET /api/v1/inventory/lots`, `POST /api/v1/inventory/lots`, `GET /api/v1/inventory/serial-numbers`, `POST /api/v1/inventory/serial-numbers`)
- [X] T038 [US3] Implement Lots & Serial Numbers UI in `frontend/src/pages/inventory/LotsAndSerialsPage.tsx` with FEFO lot table (days to expire, status indicator) and Serial Number asset tracking view

**Checkpoint**: User Stories 1, 2, and 3 work independently.

---

## Phase 6: User Story 4 - Multi-Price Lists, Markup Calculation & Cost Accounting (Priority: P4)

**Goal**: Establish acquisition costs, calculated weighted average costs (CMP), markup percentages, and manage multiple pricing tables (e.g. Varejo, Atacado) with promotional validity schedules.

**Independent Test**: Create two price lists (Tabela Varejo and Tabela Atacado), configure a base product cost, apply a target markup percentage to suggest sales prices, set a promotional price with start/end date, and verify price resolution rules.

### Tests for User Story 4

- [X] T039 [P] [US4] Create unit tests for PricingService (CMP calculation formula, markup calculator, and promotional validity schedule resolution) in `backend/src/test/kotlin/org/opencontrol/erp/catalog/service/PricingServiceTest.kt`

### Implementation for User Story 4

- [X] T040 [P] [US4] Create JPA entity `PriceList` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/domain/model/PriceList.kt` with fields `id: UUID`, `tenantId: UUID`, `code: String` (unique per tenant), `name: String`, `currency: String`, `discountOrMarkupPct: BigDecimal`, `isDefault: Boolean`, `validFrom: LocalDate?`, `validUntil: LocalDate?`, `isActive: Boolean`, `createdAt: Instant`, `updatedAt: Instant`
- [X] T041 [P] [US4] Create JPA entity `ProductPrice` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/domain/model/ProductPrice.kt` with fields `id: UUID`, `tenantId: UUID`, `priceListId: UUID`, `productId: UUID`, `salesPrice: BigDecimal`, `promotionalPrice: BigDecimal?`, `promoStart: LocalDate?`, `promoEnd: LocalDate?`, `updatedAt: Instant` (`priceListId` + `productId` unique)
- [X] T042 [P] [US4] Create Spring Data JPA repositories `PriceListRepository` and `ProductPriceRepository` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/repository/PriceListRepository.kt`
- [X] T043 [P] [US4] Create Pricing DTOs in `backend/src/main/kotlin/org/opencontrol/erp/catalog/api/dto/PricingDtos.kt` (`CreatePriceListRequest`, `PriceListResponse`, `SetProductPriceRequest`, `ProductPriceResponse`, `ResolvePriceResponse`)
- [X] T044 [US4] Implement `PricingService` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/service/PricingService.kt` for price list management, promotional validity window resolution, target markup sales price calculation, and CMP cost recalculation helper
- [X] T045 [US4] Implement `PriceListController` in `backend/src/main/kotlin/org/opencontrol/erp/catalog/api/PriceListController.kt` (`GET /api/v1/catalog/price-lists`, `POST /api/v1/catalog/price-lists`, `PUT /api/v1/catalog/price-lists/{id}/prices`, `GET /api/v1/catalog/products/{id}/prices`)
- [X] T046 [US4] Implement Price Lists and Markup UI in `frontend/src/pages/catalog/PriceListsPage.tsx` with price list builder, markup calculator, and promotional schedule picker

**Checkpoint**: User Stories 1, 2, 3, and 4 work independently.

---

## Phase 7: User Story 5 - Inventory Transactions, Stock Balance & Kardex Audit Trail (Priority: P5)

**Goal**: Provide an immutable chronological log (Kardex) of all stock movements (inbound receipts, outbound shipments, manual adjustments, inter-warehouse transfers), capturing movement type, quantity, unit cost, source/destination warehouse, and responsible user.

**Independent Test**: Execute an inbound receipt, a manual adjustment (with justification reason), and an inter-warehouse transfer, then verify that product balance and Kardex transaction history reflect all events with accurate running balance and unit costs.

### Tests for User Story 5

- [X] T047 [P] [US5] Create unit and integration tests for StockBalanceService and KardexMovementService in `backend/src/test/kotlin/org/opencontrol/erp/inventory/service/StockBalanceServiceTest.kt` and `backend/src/test/kotlin/org/opencontrol/erp/inventory/service/KardexMovementServiceTest.kt`

### Implementation for User Story 5

- [X] T048 [P] [US5] Create JPA entity `StockBalance` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/domain/model/StockBalance.kt` with fields `id: UUID`, `tenantId: UUID`, `warehouseId: UUID`, `productId: UUID`, `locationId: UUID?`, `quantityOnHand: BigDecimal`, `quantityReserved: BigDecimal`, `quantityAvailable: BigDecimal`, `version: Long` (optimistic locking), `updatedAt: Instant` (`warehouseId` + `productId` + `locationId` unique)
- [X] T049 [P] [US5] Create JPA entity `StockMovement` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/domain/model/StockMovement.kt` with fields `id: UUID`, `tenantId: UUID`, `branchId: UUID`, `movementType: StockMovementType`, `productId: UUID`, `sourceWarehouseId: UUID?`, `destinationWarehouseId: UUID?`, `locationId: UUID?`, `lotId: UUID?`, `serialNumberId: UUID?`, `quantity: BigDecimal`, `unitCost: BigDecimal`, `balanceAfter: BigDecimal`, `referenceDocument: String?`, `reasonCode: String?`, `notes: String?`, `createdByUserId: UUID`, `createdAt: Instant`
- [X] T050 [P] [US5] Create Spring Data JPA repositories `StockBalanceRepository` and `StockMovementRepository` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/repository/StockBalanceRepository.kt` and `backend/src/main/kotlin/org/opencontrol/erp/inventory/repository/StockMovementRepository.kt`
- [X] T051 [P] [US5] Create Stock Movement DTOs in `backend/src/main/kotlin/org/opencontrol/erp/inventory/api/dto/StockMovementDtos.kt` (`CreateStockMovementRequest`, `StockMovementResponse`, `StockMovementPageResponse`, `StockBalanceResponse`)
- [X] T052 [US5] Implement `StockBalanceService` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/service/StockBalanceService.kt` for atomic balance updates, optimistic lock retries, and negative stock prevention
- [X] T053 [US5] Implement `KardexMovementService` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/service/KardexMovementService.kt` orchestrating movements: inbound purchases with CMP updates, outbound sales with FEFO lot/serial reservation, manual adjustments with mandatory reason codes, and transfers
- [X] T054 [P] [US5] Implement `StockBalanceController` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/api/StockBalanceController.kt` (`GET /api/v1/inventory/balances`, `GET /api/v1/inventory/balances/products/{productId}`)
- [X] T055 [US5] Implement `StockMovementController` in `backend/src/main/kotlin/org/opencontrol/erp/inventory/api/StockMovementController.kt` (`GET /api/v1/inventory/movements`, `POST /api/v1/inventory/movements`)
- [X] T056 [P] [US5] Implement Stock Balances overview UI in `frontend/src/pages/inventory/StockBalancesPage.tsx` with multi-warehouse filters, minimum stock warning indicators, and quick manual adjustment trigger
- [X] T057 [US5] Implement Kardex Movements timeline and adjustment modal UI in `frontend/src/pages/inventory/KardexMovementsPage.tsx` and `frontend/src/components/inventory/ManualAdjustmentModal.tsx`

**Checkpoint**: All 5 user stories are complete and independently testable.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Navigation integration, bilingual localization, linting, build checks, and quickstart verification

- [X] T058 [P] Register Catalog & Inventory navigation links, badges, and routing in `frontend/src/components/layout/Sidebar.tsx` and `frontend/src/App.tsx`
- [X] T059 [P] Add internationalization strings for catalog and inventory in `frontend/src/locales/pt-BR.json` and `frontend/src/locales/en-US.json`
- [X] T060 Run backend verification `./gradlew spotlessApply check` and ensure `jacocoTestCoverageVerification` meets or exceeds the mandatory 90% threshold
- [X] T061 Run frontend verification `npm run test` and `npm run build` in `frontend/`
- [X] T062 Execute end-to-end verification following `specs/008-product-inventory-catalog/quickstart.md`
