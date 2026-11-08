# Implementation Plan: Inbound Purchases, Fiscal Invoicing, Landed Costing & Inventory Transfers

**Branch**: `009-inbound-purchases-transfers` | **Date**: 2026-11-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/009-inbound-purchases-transfers/spec.md` with explicit user design requirement:
> *"Bom deixar esse motor de calculo de custo de preço bem extensivel pois cada tipo de empresa calcula seus custos diferentes, Se puder fazer um motor igual ao excel seia otimo"*

---

## Summary

Build the end-to-end Inbound Supply Chain subsystem for OpenControl ERP covering:
1. **Purchase Orders (Pedidos ao Fornecedor)** with commercial commitment, approval workflow, and fulfillment tracking.
2. **Inbound NF-e XML Ingestion & De-Para Matching** with packaging conversions and supplier code memory.
3. **Extensible Excel-Like Landed Cost Formula Engine (`CostFormulaEngine`)** enabling companies to define custom mathematical expressions (freight apportionment, insurance, handling, customs, and statutory tax credits) using Excel functions and named variables.
4. **Physical Goods Receipt & Blind Conference** enforcing lot/serial traceability and divergence alerts.
5. **Inventory Valuation Ledger**: Double-entry ledger updates and statutory Weighted Average Cost (Custo Médio Ponderado - CMP) recalculation (Decreto 9.580/2018).
6. **Internal Warehouse & Inter-Branch Transfers** with real-time in-transit inventory control and lot preservation.

---

## Technical Context

**Language/Version**: Kotlin 2.1.x on JVM 21 LTS (Backend) & TypeScript 5.6 on React 19 (Frontend)

**Primary Dependencies**:
- Backend: Spring Boot 3.4 / 4.x, Spring Modulith, Spring Data JPA, FasterXML Jackson XML (`jackson-dataformat-xml`), Spring Expression Language (SpEL) for the secure Excel-like mathematical formula evaluator.
- Frontend: React 19, Tailwind CSS, Lucide icons, TanStack Query, Axios.

**Storage**: PostgreSQL 16 (Relational tables + JSONB for raw XML metadata and variable maps) managed via Flyway migration `V1_0_12__create_inbound_and_transfer_tables.sql`.

**Testing**: JUnit 5, MockK, Spring Boot Test, JaCoCo (strictly enforcing $\ge 90\%$ branch/line test coverage), Vitest for frontend.

**Target Platform**: Linux/Windows containers, modern desktop browsers (Chrome, Edge, Firefox, Safari).

**Project Type**: Full-stack Modular Monolith ERP.

**Performance Goals**:
- Formula expression execution: $< 5$ms per line item.
- 50-item NF-e XML ingestion and de-para matching: $< 500$ms.

**Constraints**:
- Strict multi-tenant isolation via `tenant_id` and branch awareness via `branch_id`.
- Zero security risk in formula evaluation: sandboxed expression context allowing only mathematical operators, Excel functions, and whitelisted context variables (no arbitrary code execution).

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Compliance Details |
|---|---|---|
| **I. Modular Monolith & Clean Architecture** | **PASS** | Domain logic located in `org.opencontrol.erp.inbound` with clean separation between controllers, services, repositories, and domain models. Zero cross-module direct SQL joins. |
| **II. Extensibility via SPIs & Formulas** | **PASS** | Features a dedicated `CostFormulaEngine` and extensible cost profile SPI so that tenants can author Excel-like formulas without modifying core code. |
| **III. Brazil-First Localization** | **PASS** | Native SEFAZ NF-e XML 4.00 parser, CFOPs, CSTs, ICMS/IPI/PIS/COFINS tax credits, and RIR CMP calculation. |
| **IV. Codebase Language & Nomenclature** | **PASS** | English code identifiers (`PurchaseOrder`, `InboundInvoice`, `LandedCostAllocation`, `StockTransfer`) with jurisdiction acronyms preserved (`NFe`, `CFOP`, `CST`, `CMP`, `ICMS`). |
| **V. Modern Componentized UI** | **PASS** | Built in TypeScript + React + Tailwind with responsive layouts, modal workflows, and clear status badges. |
| **VI. Financial Immutability & Auditability** | **PASS** | Double-entry stock ledger (`stock_movements`), audit logs for approvals/cancellations, and immutable transfer state transitions. |
| **VII. Minimum 90% Test Coverage Gate** | **PASS** | Comprehensive unit tests for formula evaluation, XML parsing, CMP calculation, and transfer state machines. |
| **VIII. Mandatory Documentation Gate** | **PASS** | Complete `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/inbound-api.yaml`, and `quickstart.md`. |

---

## Project Structure

### Documentation (this feature)

```text
specs/009-inbound-purchases-transfers/
├── plan.md              # This file (/speckit-plan output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── inbound-api.yaml # OpenAPI 3.0 contract
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code

```text
backend/src/main/kotlin/org/opencontrol/erp/inbound/
├── api/
│   ├── CostFormulaProfileController.kt
│   ├── PurchaseOrderController.kt
│   ├── InboundInvoiceController.kt
│   ├── StockTransferController.kt
│   └── dto/
│       ├── CostFormulaDtos.kt
│       ├── PurchaseOrderDtos.kt
│       ├── InboundInvoiceDtos.kt
│       └── StockTransferDtos.kt
├── domain/
│   ├── CostFormulaEngine.kt
│   ├── model/
│   │   ├── CostFormulaProfile.kt
│   │   ├── PurchaseOrder.kt
│   │   ├── PurchaseOrderItem.kt
│   │   ├── InboundInvoice.kt
│   │   ├── InboundInvoiceItem.kt
│   │   ├── GoodsReceipt.kt
│   │   ├── GoodsReceiptItem.kt
│   │   ├── StockTransfer.kt
│   │   ├── StockTransferItem.kt
│   │   └── SupplierProductMapping.kt
├── repository/
│   ├── CostFormulaProfileRepository.kt
│   ├── PurchaseOrderRepository.kt
│   ├── InboundInvoiceRepository.kt
│   ├── GoodsReceiptRepository.kt
│   ├── StockTransferRepository.kt
│   └── SupplierProductMappingRepository.kt
└── service/
    ├── CostFormulaService.kt
    ├── NFeXmlParserService.kt
    ├── PurchaseOrderService.kt
    ├── InboundInvoiceService.kt
    └── StockTransferService.kt

backend/src/main/resources/db/migration/
└── V1_0_12__create_inbound_and_transfer_tables.sql

frontend/src/
├── pages/
│   ├── inbound/
│   │   ├── PurchaseOrdersPage.tsx
│   │   ├── InboundInvoicesPage.tsx
│   │   ├── CostFormulasPage.tsx
│   │   └── StockTransfersPage.tsx
├── components/
│   └── inbound/
│       ├── PurchaseOrderModal.tsx
│       ├── NFeXmlUploadModal.tsx
│       ├── LandedCostSimulatorModal.tsx
│       └── TransferOrderModal.tsx
├── services/
│   └── inboundApi.ts
└── types/
    └── inbound.ts
```

---

## Complexity Tracking

*No constitutional violations identified. Standard modular monolith extension.*
