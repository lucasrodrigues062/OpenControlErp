# Tasks: CRM, Customer Master Data, Leads & Sales Pipeline

**Input**: Design documents from `specs/007-crm-sales-pipeline/` (`plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/crm-api.yaml`, `quickstart.md`)  
**Prerequisites**: `plan.md`, `spec.md`, `data-model.md`, `contracts/crm-api.yaml`  
**Tests**: Automated tests are mandatory to satisfy Constitution Principle VII ($\ge 90\%$ test coverage).  
**Organization**: Tasks are grouped strictly by user story to enable independent implementation, testing, and incremental delivery.

---

## Format: `[TaskID] [P?] [Story?] Description with file path`

- **[P]**: Parallelizable (different files, no conflicting dependencies)
- **[Story]**: User story code (`[US1]`, `[US2]`, `[US3]`, `[US4]`)
- Every task includes the exact file path and specific acceptance constraints.

---

## Phase 1: Setup (Database Schema & Infrastructure)

**Purpose**: Database migration and security permission framework for CRM.

- [X] T001 Create Flyway database migration script `V1_0_10__create_crm_tables.sql` in `backend/src/main/resources/db/migration/V1_0_10__create_crm_tables.sql` defining tables `customer_categories`, `economic_groups`, `customers`, `leads`, `sales_pipelines`, `pipeline_stages`, `deals`, and `crm_activities` with all constraints, foreign keys, and indexes per `data-model.md`.
- [X] T002 Register CRM security permissions (`CRM_READ`, `CRM_WRITE`, `CRM_ADMIN`) and assign them to the Administrator role seed in `backend/src/main/resources/db/migration/V1_0_10__create_crm_tables.sql`.
- [X] T003 [P] Define TypeScript domain interfaces and DTO types in `frontend/src/types/crm.ts` matching OpenAPI schemas from `specs/007-crm-sales-pipeline/contracts/crm-api.yaml`.
- [X] T004 [P] Create frontend API client in `frontend/src/services/crmApi.ts` using Axios with endpoints for customers, categories, economic groups, leads, pipelines, deals, and activities.

---

## Phase 2: Foundational (Statutory Validation & Shared Domain Core)

**Purpose**: Brazilian Modulo 11 document validation (CPF & CNPJ) and domain event models required across all CRM user stories.

- [X] T005 [P] Create unit test suite `BrazilianDocumentValidatorTest.kt` in `backend/src/test/kotlin/br/com/opencontrol/crm/domain/validation/BrazilianDocumentValidatorTest.kt` covering valid/invalid check digits for CPF (11 digits) and CNPJ (14 digits), repetitive numbers, and unmasking.
- [X] T006 [P] Implement `BrazilianDocumentValidator.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/domain/validation/BrazilianDocumentValidator.kt` verifying Modulo 11 check digits for CPF and CNPJ.
- [X] T007 [P] Create CRM domain events in `backend/src/main/kotlin/br/com/opencontrol/crm/domain/events/CrmDomainEvents.kt` (`CustomerCreatedEvent`, `CustomerUpdatedEvent`, `LeadConvertedEvent`, `DealWonEvent`, `DealLostEvent`).

**Checkpoint**: Foundational document validator and database schema ready. User story implementation can now proceed.

---

## Phase 3: User Story 1 - Customer Master Data, Categorization & Economic Groups (Priority: P1) 🎯 MVP

**Goal**: Deliver complete Customer Master Data (PF/PJ) with Modulo 11 validation, addresses, credit limit tracking, Customer Categories, and Economic Groups.

**Independent Test**: Register Customer Categories and Economic Groups, register a Legal Entity with valid CNPJ and an Individual with valid CPF, verify rejection of invalid check digits, and inspect customer profiles with masked formatting.

### Tests for User Story 1
- [X] T008 [P] [US1] Create unit tests for Customer, Category, and Economic Group services in `backend/src/test/kotlin/br/com/opencontrol/crm/service/CustomerServiceTest.kt` testing CRUD, Modulo 11 validation, tenant duplicate rejection, and credit limit updates.
- [X] T009 [P] [US1] Create REST API integration tests for customer endpoints in `backend/src/test/kotlin/br/com/opencontrol/crm/web/CustomerControllerIntegrationTest.kt` verifying `GET /api/v1/crm/customers`, `POST /api/v1/crm/customers`, `PUT /api/v1/crm/customers/{id}`, and category/group APIs.

### Backend Implementation for User Story 1
- [X] T010 [P] [US1] Create JPA entities `CustomerCategory.kt` and `EconomicGroup.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/domain/model/` with audit columns and tenant isolation.
- [X] T011 [P] [US1] Create Spring Data JPA repositories `CustomerCategoryRepository.kt` and `EconomicGroupRepository.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/repository/`.
- [X] T012 [P] [US1] Create JPA entity `Customer.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/domain/model/Customer.kt` supporting `type` (`INDIVIDUAL`, `LEGAL_ENTITY`), unmasked `document`, primary/billing/delivery addresses, credit limits, and relations to category and economic group.
- [X] T013 [P] [US1] Create Spring Data JPA repository `CustomerRepository.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/repository/CustomerRepository.kt` with paginated search by query, document, category, economic group, and tenant.
- [X] T014 [US1] Implement `CustomerCategoryService.kt` and `EconomicGroupService.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/service/`.
- [X] T015 [US1] Implement `CustomerService.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/service/CustomerService.kt` incorporating `BrazilianDocumentValidator`, tenant duplicate check, and audit event dispatch.
- [X] T016 [P] [US1] Implement DTOs in `backend/src/main/kotlin/br/com/opencontrol/crm/web/dto/CustomerDtos.kt` with `@get:JsonProperty` annotations and document formatting helpers (`formattedDocument`).
- [X] T017 [US1] Implement REST controllers `CustomerCategoryController.kt`, `EconomicGroupController.kt`, and `CustomerController.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/web/rest/`.

### Frontend Implementation for User Story 1
- [X] T018 [P] [US1] Create modal component `CustomerCategoryModal.tsx` in `frontend/src/components/crm/CustomerCategoryModal.tsx` for creating and managing categories with color pickers and discount percentages.
- [X] T019 [P] [US1] Create modal component `EconomicGroupModal.tsx` in `frontend/src/components/crm/EconomicGroupModal.tsx` for managing holding groups and consolidated credit limits.
- [X] T020 [US1] Create modal component `CustomerFormModal.tsx` in `frontend/src/components/crm/CustomerFormModal.tsx` supporting Individual/Legal Entity toggle, masked CPF/CNPJ input, ViaCEP auto-fill, addresses, category/group selectors, and credit limit fields.
- [X] T021 [US1] Create page `CustomersPage.tsx` in `frontend/src/pages/crm/CustomersPage.tsx` with search bar, category/group filters, status badges, pagination, action dropdowns, and quick-add modals.
- [X] T022 [US1] Create detailed view page `CustomerDetailPage.tsx` in `frontend/src/pages/crm/CustomerDetailPage.tsx` rendering customer 360 overview (statutory data, addresses, credit status, and Custom Fields tab).

**Checkpoint**: User Story 1 (MVP) is fully functional and testable end-to-end.

---

## Phase 4: User Story 2 - Lead Capture, Qualification & Conversion (Priority: P2)

**Goal**: Capture leads with acquisition sources, qualify their commercial readiness, support disqualification with mandatory reason, and convert qualified leads into Customers and Deals in 1 click.

**Independent Test**: Create an inbound lead, update status through qualification stages, verify disqualification reason requirement, and trigger 1-click conversion producing an active customer account and pipeline opportunity.

### Tests for User Story 2
- [X] T023 [P] [US2] Create unit tests for Lead lifecycle and conversion in `backend/src/test/kotlin/br/com/opencontrol/crm/service/LeadServiceTest.kt` verifying `NEW` -> `IN_QUALIFICATION` -> `QUALIFIED` -> `CONVERTED`, disqualification validation, and conversion atomicity.
- [X] T024 [P] [US2] Create integration tests for lead REST endpoints in `backend/src/test/kotlin/br/com/opencontrol/crm/web/LeadControllerIntegrationTest.kt` testing `POST /api/v1/crm/leads`, `POST /api/v1/crm/leads/{id}/disqualify`, and `POST /api/v1/crm/leads/{id}/convert`.

### Backend Implementation for User Story 2
- [X] T025 [P] [US2] Create JPA entity `Lead.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/domain/model/Lead.kt` with fields `status`, `source`, `estimatedBudget`, `disqualificationReason`, and links to converted customer/deal.
- [X] T026 [P] [US2] Create Spring Data JPA repository `LeadRepository.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/repository/LeadRepository.kt` with filters by tenant, status, and source.
- [X] T027 [US2] Implement `LeadService.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/service/LeadService.kt` orchestrating lead mutations, disqualification validation, and transactional 1-click conversion invoking `CustomerService` and `DealService`.
- [X] T028 [P] [US2] Implement DTOs in `backend/src/main/kotlin/br/com/opencontrol/crm/web/dto/LeadDtos.kt` (`CreateLeadRequest`, `LeadResponse`, `ConvertLeadRequest`, `LeadConversionResponse`).
- [X] T029 [US2] Implement REST controller `LeadController.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/web/rest/LeadController.kt`.

### Frontend Implementation for User Story 2
- [X] T030 [P] [US2] Create modal component `LeadFormModal.tsx` in `frontend/src/components/crm/LeadFormModal.tsx` for capturing prospective leads with source channels and budget estimates.
- [X] T031 [P] [US2] Create modal component `ConvertLeadModal.tsx` in `frontend/src/components/crm/ConvertLeadModal.tsx` allowing the SDR to specify customer document, initial deal title, and pipeline stage before conversion.
- [X] T032 [US2] Create page `LeadsPage.tsx` in `frontend/src/pages/crm/LeadsPage.tsx` with funnel status tabs (`NEW`, `IN_QUALIFICATION`, `QUALIFIED`, `DISQUALIFIED`, `CONVERTED`), search, disqualification modal, and 1-click conversion action.

**Checkpoint**: User Stories 1 and 2 work independently and can be demonstrated together.

---

## Phase 5: User Story 3 - Visual Sales Pipeline & Deal Opportunities (Priority: P3)

**Goal**: Provide interactive sales funnels, visual Kanban drag-and-drop board, real-time stage metrics calculation, and mandatory loss reason capture when dropped into `CLOSED_LOST`.

**Independent Test**: Create a custom pipeline and stages, create deals with monetary values, drag-and-drop deal cards between stages, verify real-time metric updates, and verify that moving to `CLOSED_LOST` demands a loss reason.

### Tests for User Story 3
- [X] T033 [P] [US3] Create unit tests for Pipeline, Stage, and Deal state machines in `backend/src/test/kotlin/br/com/opencontrol/crm/service/DealServiceTest.kt` verifying stage transitions, won/lost timestamps, mandatory loss reasons, and aggregate metrics.
- [X] T034 [P] [US3] Create integration tests for pipeline and deal endpoints in `backend/src/test/kotlin/br/com/opencontrol/crm/web/DealControllerIntegrationTest.kt` testing `PATCH /api/v1/crm/deals/{id}/stage` and `GET /api/v1/crm/deals/metrics`.

### Backend Implementation for User Story 3
- [X] T035 [P] [US3] Create JPA entities `SalesPipeline.kt` and `PipelineStage.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/domain/model/` with stage win probabilities and SLA stagnant days.
- [X] T036 [P] [US3] Create JPA entity `Deal.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/domain/model/Deal.kt` with estimated value, stage references, status (`OPEN`, `WON`, `LOST`), and `lossReason`.
- [X] T037 [P] [US3] Create Spring Data JPA repositories `SalesPipelineRepository.kt`, `PipelineStageRepository.kt`, and `DealRepository.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/repository/`.
- [X] T038 [US3] Implement `SalesPipelineService.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/service/SalesPipelineService.kt` with default pipeline initialization for new tenants.
- [X] T039 [US3] Implement `DealService.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/service/DealService.kt` handling deal creation, optimistic stage changes, loss reason validation, and aggregate pipeline metric calculations.
- [X] T040 [P] [US3] Implement DTOs in `backend/src/main/kotlin/br/com/opencontrol/crm/web/dto/DealDtos.kt` and `PipelineDtos.kt`.
- [X] T041 [US3] Implement REST controllers `SalesPipelineController.kt` and `DealController.kt` in `backend/src/main/kotlin/br/com/opencontrol/crm/web/rest/`.

### Frontend Implementation for User Story 3
- [X] T042 [P] [US3] Create Kanban card component `KanbanCard.tsx` in `frontend/src/components/crm/KanbanCard.tsx` rendering deal title, customer name, monetary value in BRL, days in stage, and stagnant warning badges.
- [X] T043 [P] [US3] Create Kanban column component `KanbanColumn.tsx` in `frontend/src/components/crm/KanbanColumn.tsx` calculating column deal totals and probability-weighted values.
- [X] T044 [P] [US3] Create modal component `DealLossReasonModal.tsx` in `frontend/src/components/crm/DealLossReasonModal.tsx` prompting for reason when dropped into `CLOSED_LOST`.
- [X] T045 [P] [US3] Create modal component `DealFormModal.tsx` in `frontend/src/components/crm/DealFormModal.tsx` for creating and editing deals.
- [X] T046 [US3] Create Kanban board component `KanbanBoard.tsx` in `frontend/src/components/crm/KanbanBoard.tsx` with drag-and-drop listeners, optimistic stage movement, and summary metrics header bar.
- [X] T047 [US3] Create page `SalesPipelinePage.tsx` in `frontend/src/pages/crm/SalesPipelinePage.tsx` providing view switching between Kanban board and paginated list table, pipeline selector, and owner filter.

**Checkpoint**: User Stories 1, 2, and 3 are completely integrated and functional.

---

## Phase 6: User Story 4 - Commercial Interactions, Timeline & Extension Integration (Priority: P4)

**Goal**: Deliver chronological activity timeline logging (calls, WhatsApp, meetings, notes, tasks) and integrate dynamic Custom Fields for entity `CUSTOMER`.

**Independent Test**: Register an interaction note on a customer profile, verify appearance in descending chronological order, define a Custom Field for `CUSTOMER` in settings, and confirm dynamic rendering in the customer form and detail views.

### Tests for User Story 4
- [X] T048 [P] [US4] Create unit tests for CRM activities in `backend/src/test/kotlin/org/opencontrol/erp/crm/service/CrmActivityServiceTest.kt` verifying timeline queries by entity type and ID.
- [X] T049 [P] [US4] Create integration tests for activity endpoints in `backend/src/test/kotlin/org/opencontrol/erp/crm/api/CrmActivityControllerTest.kt`.

### Backend Implementation for User Story 4
- [X] T050 [P] [US4] Create JPA entity `CrmActivity.kt` in `backend/src/main/kotlin/org/opencontrol/erp/crm/domain/model/CrmActivity.kt` supporting `entityType` (`CUSTOMER`, `LEAD`, `DEAL`), `activityType` (`NOTE`, `CALL`, `MEETING`, `WHATSAPP`, `TASK`), and author reference.
- [X] T051 [P] [US4] Create Spring Data JPA repository `CrmActivityRepository.kt` in `backend/src/main/kotlin/org/opencontrol/erp/crm/repository/CrmActivityRepository.kt`.
- [X] T052 [US4] Implement `CrmActivityService.kt` in `backend/src/main/kotlin/org/opencontrol/erp/crm/service/CrmActivityService.kt`.
- [X] T053 [P] [US4] Implement DTOs in `backend/src/main/kotlin/org/opencontrol/erp/crm/api/dto/ActivityDtos.kt`.
- [X] T054 [US4] Implement REST controller `CrmActivityController.kt` in `backend/src/main/kotlin/org/opencontrol/erp/crm/api/CrmActivityController.kt`.

### Frontend Implementation for User Story 4
- [X] T055 [P] [US4] Create timeline component `ActivityTimeline.tsx` in `frontend/src/components/crm/ActivityTimeline.tsx` rendering activity history with type icons, timestamp formatting, author info, and quick-log form.
- [X] T056 [US4] Embed `ActivityTimeline.tsx` into `CustomerDetailPage.tsx`, `LeadDetailPage` or lead detail modal, and deal inspection modal.
- [X] T057 [US4] Embed dynamic Custom Fields section in `CustomerFormModal.tsx` and `CustomerDetailPage.tsx` using `customFieldsApi` for entity `CUSTOMER`.

**Checkpoint**: All 4 user stories are fully implemented, tested, and integrated.

---

## Phase 7: Polish, Navigation & Verification

**Purpose**: App navigation integration, internationalization labels, quickstart scenario verification, and coverage gate enforcement.

- [X] T058 Update navigation sidebar in `frontend/src/components/layout/AppLayout.tsx` and routing in `frontend/src/App.tsx` adding the CRM navigation section (Clientes, Leads, Pipeline de Vendas).
- [X] T059 Add bilingual dictionary entries in `frontend/src/locales/` for English (`en-US`) and Portuguese (`pt-BR`) covering all CRM labels, filters, and status badges.
- [X] T060 Execute end-to-end scenarios 1 through 4 from `specs/007-crm-sales-pipeline/quickstart.md` and verify all acceptance criteria pass.
- [X] T061 Run `./gradlew test jacocoTestReport jacocoTestCoverageVerification` in `backend/` and `npm run test` in `frontend/` ensuring $\ge 90\%$ test coverage per Constitution Principle VII.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories.
- **User Story 1 (Phase 3 - P1 MVP)**: Depends on Phase 2. Can be delivered and verified as a standalone increment.
- **User Story 2 (Phase 4 - P2)**: Depends on Phase 2; integrates with Customer and Deal creation from US1 and US3.
- **User Story 3 (Phase 5 - P3)**: Depends on Phase 2 and Customer entity from US1.
- **User Story 4 (Phase 6 - P4)**: Depends on Entities from US1, US2, and US3.
- **Polish & Verification (Phase 7)**: Depends on completion of all user story phases.

### Parallel Opportunities

- **Phase 1**: T003 (types) and T004 (API client) can run in parallel with T001 (migration).
- **Phase 2**: T005 (test) and T006 (validator) can run in parallel with T007 (events).
- **Phase 3 (US1)**:
  - T008 (unit test) and T009 (integration test) can run in parallel.
  - T010, T011, T012, T013 (models and repos) can be created in parallel.
  - T018 and T019 (frontend modals) can run in parallel with backend controllers.
- **Phase 4 (US2)**:
  - T023 and T024 (tests) can run in parallel.
  - T025, T026, T028 (models, repos, DTOs) can run in parallel.
  - T030 and T031 (frontend modals) can run in parallel.
- **Phase 5 (US3)**:
  - T033 and T034 (tests) can run in parallel.
  - T035, T036, T037, T040 (models, repos, DTOs) can run in parallel.
  - T042, T043, T044, T045 (Kanban components) can run in parallel.
- **Phase 6 (US4)**:
  - T048 and T049 (tests) can run in parallel.
  - T050, T051, T053 (models, repos, DTOs) can run in parallel.
  - T055 (ActivityTimeline) can run in parallel.

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Complete Phase 1: Setup (`V1_0_10__create_crm_tables.sql`, permissions, types, api client).
2. Complete Phase 2: Foundational (`BrazilianDocumentValidator` with Modulo 11 check digits).
3. Complete Phase 3: User Story 1 (Customer Master Data, Categories, Economic Groups, Address management, credit limit).
4. **Validate MVP**: Test User Story 1 independently with Brazilian PF/PJ registration scenarios.

### Incremental Delivery
1. Foundation + US1 (MVP) -> Full Customer Master Data operational.
2. US2 -> Lead capture, qualification, and 1-click conversion into Customers.
3. US3 -> Visual Kanban pipeline, deal management, stage progression, and loss reasons.
4. US4 -> Timeline interaction logging and dynamic Custom Fields integration.
5. Polish & Verification -> Navigation, i18n, and 90% test coverage gate.
