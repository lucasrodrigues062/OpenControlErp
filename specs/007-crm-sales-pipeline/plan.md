# Implementation Plan: CRM, Customer Master Data, Leads & Sales Pipeline

**Branch**: `007-crm-sales-pipeline` | **Date**: 2026-11-02 | **Spec**: [specs/007-crm-sales-pipeline/spec.md](specs/007-crm-sales-pipeline/spec.md)

**Input**: Feature specification from `specs/007-crm-sales-pipeline/spec.md` with legacy ERP reference DDL (`dbo.Cli_For` from `dbCronos`).

---

## Summary

Deliver a comprehensive, production-ready CRM module for OpenControl ERP MVP. The module decomposes the 160+ column legacy Brazilian ERP flat table (`dbo.Cli_For`) into high-cohesion Domain-Driven Design (DDD) aggregates:
1. **Customer Master Data**: Individuals (`INDIVIDUAL`) and Legal Entities (`LEGAL_ENTITY`) with strict Modulo 11 CPF and CNPJ validation, Brazilian address support (primary, billing, delivery), credit limit tracking, Customer Categories (`CustomerCategory`), and corporate conglomerates (`EconomicGroup`).
2. **Lead Management**: Inbound/outbound lead acquisition, status progression, disqualification reasons, and 1-click conversion into an active Customer and initial Opportunity.
3. **Visual Sales Funnel**: Configurable Sales Pipelines and stages, Kanban drag-and-drop progression with optimistic updates, loss reason capture on `CLOSED_LOST`, and real-time pipeline forecasting metrics.
4. **Interaction Timeline & Custom Fields**: Chronological CRM activity logging (calls, WhatsApp, meetings, notes, tasks) and integration with the dynamic Custom Fields engine (`targetEntity = 'CUSTOMER'`).

---

## Technical Context

**Language/Version**: Kotlin 2.2 targeting JVM 21 LTS (Backend) | TypeScript 5.8+ (Frontend)  
**Primary Dependencies**: Spring Boot 4.1.2, Spring Modulith, Spring Data JPA, Hibernate, Flyway, PostgreSQL JDBC, Jackson | React 19, Vite 6, Tailwind CSS, Lucide React, TanStack Query, Axios  
**Storage**: PostgreSQL 16+ via Flyway migration `V1_0_10__create_crm_tables.sql`  
**Testing**: JUnit 5, Mockito Kotlin, AssertJ, Spring MockMvc, Testcontainers PostgreSQL | Vitest, React Testing Library  
**Target Platform**: Web application (Linux / Docker server runtime, responsive desktop browser interface)  
**Project Type**: Web Application (Backend Spring Boot API + Frontend React Vite SPA)  
**Performance Goals**:
- Customer lookup & search: <100ms p95 latency
- 1-Click Lead Conversion transaction: <250ms
- Kanban drag-and-drop response: <50ms UI update for pipelines up to 500 deals
- Database indexing: B-tree indexes on `(tenant_id, document)`, `(tenant_id, code)`, and foreign keys  
**Constraints**: Zero invalid CPF or CNPJ persisted (Modulo 11 enforced); strict multi-tenancy and branch scoping; append-only audit trail integration.  
**Scale/Scope**: 5 core domain entities + 3 reference entities; 18 REST endpoints; 4 responsive UI pages/views (Customers, Leads, Pipeline Kanban, Categories & Economic Groups).

---

## Constitution Check

*GATE: All principles evaluated against OpenControl ERP Constitution v1.3.0. Gate status: PASSED.*

| Constitution Principle | Evaluation & Compliance Strategy | Gate Status |
|---|---|---|
| **I. Modular Monolith & Clean Architecture** | CRM is implemented as an autonomous module `br.com.opencontrol.crm` with clean domain entities, ports, services, and web adapters. Cross-module queries (e.g., from Sales or Invoicing) interact via published service contracts or domain events (`CUSTOMER_CREATED`, `DEAL_WON`). No direct cross-joins. | **PASS** |
| **II. Extensibility via Plugin & App Architecture** | CRM lifecycle hooks emit events before/after deal stage transitions, customer creation, and lead conversion. Supports dynamic Custom Fields for `CUSTOMER`. | **PASS** |
| **III. Brazil-First Localization with Multi-Jurisdiction Abstraction** | Strict Brazilian Modulo 11 check digits for CPF (11) and CNPJ (14); statutory address fields (CEP, IBGE, UF); Inscrição Estadual (IE) and Inscrição Municipal (IM); ICMS taxpayer classification. Multi-currency and country code abstractions are preserved. | **PASS** |
| **IV. Codebase Language & Jurisdiction Nomenclature Standards** | Codebase, classes, database schemas, REST APIs, and tests are 100% in English (`Customer`, `Lead`, `Deal`, `SalesPipeline`, `CrmActivity`). Statutory acronyms without direct English translation remain standard (`Cpf`, `Cnpj`, `Ie`, `Ibge`). | **PASS** |
| **V. Modern Componentized UI with Design System Discipline** | Built with React, Tailwind CSS, Lucide icons, and accessible modal dialogs. Features an interactive Kanban board with responsive drag-and-drop, filter bars, and bilingual support (`pt-BR` and `en-US`). | **PASS** |
| **VI. Transactional Integrity, Financial Immutability & Auditability** | Deal wins/losses, customer updates, and lead conversions trigger immutable audit events through the transactional event outbox. | **PASS** |
| **VII. Minimum 90% Automated Test Coverage Gate** | Minimum 90% line and branch test coverage enforced via `jacocoTestCoverageVerification`. Unit tests cover Modulo 11 algorithms, state transitions, conversion workflows, and controllers. | **PASS** |
| **VIII. Mandatory Feature Documentation & Contract Transparency** | All artifacts generated: `spec.md`, `research.md`, `data-model.md`, `crm-api.yaml` (OpenAPI 3.0), and `quickstart.md`. KDoc docstrings on all public classes and methods. | **PASS** |

---

## Project Structure

### Documentation (`specs/007-crm-sales-pipeline/`)

```text
specs/007-crm-sales-pipeline/
├── spec.md                     # Feature specification (User Stories 1-4, Requirements, Success Criteria)
├── plan.md                     # This implementation plan
├── research.md                 # Phase 0: Legacy DDL decomposition, Modulo 11, Kanban architecture
├── data-model.md               # Phase 1: PostgreSQL ER diagram, schemas, and state transitions
├── contracts/
│   └── crm-api.yaml            # Phase 1: OpenAPI 3.0 REST specification
├── quickstart.md               # Phase 1: Runnable end-to-end verification scenarios
├── checklists/
│   └── requirements.md         # Requirements traceability checklist
└── tasks.md                    # Phase 2: Actionable tasks generated by /speckit-tasks (pending)
```

### Source Code (`backend/` & `frontend/`)

```text
backend/
├── src/main/kotlin/br/com/opencontrol/
│   └── crm/
│       ├── domain/
│       │   ├── model/
│       │   │   ├── Customer.kt
│       │   │   ├── CustomerCategory.kt
│       │   │   ├── EconomicGroup.kt
│       │   │   ├── Lead.kt
│       │   │   ├── SalesPipeline.kt
│       │   │   ├── PipelineStage.kt
│       │   │   ├── Deal.kt
│       │   │   └── CrmActivity.kt
│       │   ├── validation/
│       │   │   └── BrazilianDocumentValidator.kt
│       │   └── events/
│       │       ├── CustomerEvents.kt
│       │       ├── LeadEvents.kt
│       │       └── DealEvents.kt
│       ├── repository/
│       │   ├── CustomerRepository.kt
│       │   ├── CustomerCategoryRepository.kt
│       │   ├── EconomicGroupRepository.kt
│       │   ├── LeadRepository.kt
│       │   ├── SalesPipelineRepository.kt
│       │   ├── PipelineStageRepository.kt
│       │   ├── DealRepository.kt
│       │   └── CrmActivityRepository.kt
│       ├── service/
│       │   ├── CustomerService.kt
│       │   ├── CustomerCategoryService.kt
│       │   ├── EconomicGroupService.kt
│       │   ├── LeadService.kt
│       │   ├── SalesPipelineService.kt
│       │   ├── DealService.kt
│       │   └── CrmActivityService.kt
│       └── web/
│           ├── dto/
│           │   ├── CustomerDtos.kt
│           │   ├── LeadDtos.kt
│           │   ├── DealDtos.kt
│           │   └── ActivityDtos.kt
│           └── rest/
│               ├── CustomerController.kt
│               ├── CustomerCategoryController.kt
│               ├── EconomicGroupController.kt
│               ├── LeadController.kt
│               ├── SalesPipelineController.kt
│               ├── DealController.kt
│               └── CrmActivityController.kt
├── src/main/resources/db/migration/
│   └── V1_0_10__create_crm_tables.sql
└── src/test/kotlin/br/com/opencontrol/crm/
    ├── domain/
    │   └── BrazilianDocumentValidatorTest.kt
    ├── service/
    │   ├── CustomerServiceTest.kt
    │   ├── LeadServiceTest.kt
    │   └── DealServiceTest.kt
    └── web/
        └── CrmControllersIntegrationTest.kt

frontend/
├── src/
│   ├── services/
│   │   └── crmApi.ts
│   ├── pages/
│   │   └── crm/
│   │       ├── CustomersPage.tsx
│   │       ├── CustomerDetailPage.tsx
│   │       ├── LeadsPage.tsx
│   │       ├── SalesPipelinePage.tsx
│   │       └── CrmSettingsPage.tsx
│   ├── components/
│   │   └── crm/
│   │       ├── CustomerFormModal.tsx
│   │       ├── LeadFormModal.tsx
│   │       ├── ConvertLeadModal.tsx
│   │       ├── DealFormModal.tsx
│   │       ├── DealLossReasonModal.tsx
│   │       ├── KanbanBoard.tsx
│   │       ├── KanbanColumn.tsx
│   │       ├── KanbanCard.tsx
│   │       └── ActivityTimeline.tsx
│   └── types/
│       └── crm.ts
└── tests/
    └── crm/
        ├── CustomerForm.test.tsx
        └── KanbanBoard.test.tsx
```

---

## Complexity Tracking

*No constitutional violations identified. Design adheres fully to Principles I through VIII.*
