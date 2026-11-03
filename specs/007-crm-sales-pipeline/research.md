# Phase 0 Research & Technical Decisions: CRM, Leads & Sales Pipeline

**Feature Branch**: `007-crm-sales-pipeline`  
**Date**: 2026-11-02  
**Spec Reference**: `specs/007-crm-sales-pipeline/spec.md`  
**Domain Context**: Legacy ERP comparison (`dbo.Cli_For` from dbCronos) vs Clean Architecture & Modern Brazilian ERP Standards

---

## 1. Domain Modeling: Decomposing Legacy Flat Table (`dbo.Cli_For`) into DDD Aggregates

### Context & Problem
The reference legacy ERP schema (`dbo.Cli_For`) contains ~160 columns spanning customer identity, physical person marital/guarantor records, banking references, commercial references, credit limit queries, multiple physical addresses (billing, delivery, collection), and category/economic group references in a single flat table.
Flat schemas of this nature create high coupling, sparse NULL-heavy tables, complex mutations, and severe auditability challenges.

### Decision
Following OpenControl ERP Constitution Principle I (Modular Monolith & Clean Architecture) and Principle IV (Codebase Language Standards), decompose the legacy commercial domain into high-cohesion Domain-Driven Design (DDD) aggregates:
1. **`Customer` Aggregate**: The core master data entity. Represents both Individuals (`INDIVIDUAL` / Pessoa Física) and Legal Entities (`LEGAL_ENTITY` / Pessoa Jurídica).
   - Core statutory fields: `type`, `document` (clean unmasked CPF or CNPJ), `legal_name` (Razão Social / Nome Completo), `trade_name` (Nome Fantasia / Apelido), `state_registration` (Inscrição Estadual / RG), `is_state_registration_exempt` (Isento de IE), `municipal_registration` (Inscrição Municipal), `icms_taxpayer_type` (`CONTRIBUTOR`, `NON_CONTRIBUTOR`, `EXEMPT`).
   - Addresses: Embedded structured value objects (`address`, `billing_address`, `delivery_address`) capturing logradouro, número, complemento, bairro, cidade, UF, CEP, and IBGE code.
   - Financial & Credit: `credit_limit`, `credit_status` (`APPROVED`, `SUSPENDED`, `BLOCKED`).
   - Foreign references: `category_id`, `economic_group_id`, `branch_id`, `tenant_id`.
   - Niche/industry-specific fields (e.g., guarantor, spouse, banking references from legacy `dbo.Cli_For`): Modeled through the native dynamic **Custom Fields** engine (`target_entity = 'CUSTOMER'`), avoiding database schema bloat while providing full customization.
2. **`CustomerCategory` Aggregate**: Segment taxonomy (`id`, `tenant_id`, `code`, `name`, `color_hex`, `default_discount_pct`, `is_active`).
3. **`EconomicGroup` Aggregate**: Corporate conglomerates and holding groups (`id`, `tenant_id`, `code`, `name`, `holding_cnpj`, `consolidated_credit_limit`, `is_active`).
4. **`Lead` Aggregate**: Top-of-funnel prospects (`id`, `tenant_id`, `branch_id`, `name`, `company_name`, `email`, `phone`, `status` [NEW, IN_QUALIFICATION, QUALIFIED, DISQUALIFIED, CONVERTED], `source` [WEBSITE, INBOUND, EVENT, OUTBOUND, REFERRAL, SOCIAL_MEDIA, OTHER], `assigned_user_id`, `disqualification_reason`, `converted_customer_id`, `converted_deal_id`).
5. **`SalesPipeline` & `PipelineStage` Aggregates**: Configurable funnels (`SalesPipeline`: `id`, `name`, `code`, `is_default`, `is_active`; `PipelineStage`: `id`, `pipeline_id`, `name`, `order_index`, `win_probability_pct`, `color_hex`, `max_stagnant_days`).
6. **`Deal` Aggregate**: Opportunities (`id`, `tenant_id`, `branch_id`, `pipeline_id`, `stage_id`, `customer_id`, `lead_id`, `title`, `estimated_value`, `expected_close_date`, `status` [OPEN, WON, LOST], `loss_reason`, `assigned_user_id`).
7. **`CrmActivity` Aggregate**: Interaction timeline (`id`, `tenant_id`, `entity_type` [CUSTOMER, LEAD, DEAL], `entity_id`, `type` [NOTE, CALL, MEETING, WHATSAPP, TASK], `summary`, `description`, `performed_at`, `author_user_id`).

### Alternatives Considered
- *Direct 1:1 migration of `dbo.Cli_For`*: Rejected because storing 160 sparse columns violates DDD, degrades PostgreSQL row density, and breaks clean modular boundaries.
- *Separate tables for PF (`CustomerIndividual`) and PJ (`CustomerCompany`)*: Rejected because sales pipelines, quotes, invoices, and accounting treat buyers uniformly via polymorphic interfaces, while customer addresses and credit limits apply equally to both. A discriminator enum `CustomerType` with polymorphic validation is standard enterprise practice.

---

## 2. Document Validation: Brazilian Modulo 11 (CPF & CNPJ)

### Context & Problem
Customer registration must validate both CPF (Cadastro de Pessoas Físicas - 11 digits) and CNPJ (Cadastro Nacional da Pessoa Jurídica - 14 digits) with exact Modulo 11 check digits. Duplicate documents within the same tenant must be rejected. Unmasked digits should be stored for query indexing and uniqueness constraints.

### Decision
1. Reuse and generalize the existing `CnpjValidator` into a unified `BrazilianDocumentValidator` component with dedicated validation for:
   - **CPF**: Exactly 11 numeric digits, checks against known invalid repetitive sequences (`00000000000`, `11111111111`, etc.), and calculates both check digits using weights `10..2` and `11..2` modulo 11.
   - **CNPJ**: Exactly 14 numeric digits, checks against repetitive sequences, calculates check digits using weights `5,4,3,2,9..2` and `6,5,4,3,2,9..2` modulo 11.
2. Persist clean numeric digits (`VARCHAR(14)`) in PostgreSQL with a composite unique index: `UNIQUE (tenant_id, document)`.
3. Provide presentation masking helpers in Kotlin DTOs and TypeScript frontend:
   - CPF: `###.###.###-##`
   - CNPJ: `##.###.###/####-##`
   - CEP: `#####-###`
   - Phone: `(##) #####-####` or `(##) ####-####`

### Alternatives Considered
- *Storing formatted strings in database*: Rejected because punctuation variations (`.` vs `-` vs `/`) cause duplicate registration vulnerabilities and slower index scans.
- *External API validation (e.g., Receita Federal / Serpro)*: Defer live Receita Federal lookup to an optional async enrichment SPI. Local Modulo 11 check digit verification is instant, offline-capable, and non-blocking.

---

## 3. Visual Pipeline Kanban: Interaction & State Transitions

### Context & Problem
The sales team manages deals through a drag-and-drop Kanban interface. Moving a card updates the stage and recalculates stage values. Moving to `CLOSED_LOST` requires capturing a reason. Moving to `CLOSED_WON` records a win timestamp. Pipelines can have up to 500 active deals.

### Decision
1. **Frontend**:
   - Use standard HTML5 Drag & Drop or lightweight `@hello-pangea/dnd` / native draggable components already established in React.
   - Optimistic UI updates: When a card is dropped into another stage column, update local stage status immediately, trigger a background PATCH request `PATCH /api/v1/crm/deals/{id}/stage` with `{ stageId: "..." }`.
   - Interception modal: If the target stage is marked as Lost (`status = LOST`), prompt with a dialog requiring `lossReason` before committing.
   - Real-time recalculation: Stage summaries (Count, Total Value, Weighted Probability Value) compute reactively in the UI.
2. **Backend**:
   - `DealService.updateStage(id, newStageId, lossReason)` checks:
     - Tenant and branch scoping.
     - Valid stage transition within the same pipeline.
     - If new stage marks deal as `LOST`, `lossReason` is mandatory.
     - If new stage marks deal as `WON`, set `closedAt = Instant.now()`.
     - Publishes domain event `DEAL_STAGE_CHANGED` or `DEAL_WON` / `DEAL_LOST` through the Outbox table for audit logging.

### Alternatives Considered
- *Full WebSocket / SSE synchronization*: Overkill for MVP single-user / multi-user standard concurrency. Standard REST with optimistic UI and TanStack Query cache invalidation is robust and fast.

---

## 4. Multi-Branch & Tenant Isolation

### Context & Problem
OpenControl ERP supports multi-tenancy and multi-branch (`CompanyBranch`) operations. Commercial records (customers, leads, deals) must respect branch boundaries while allowing tenant administrators consolidated visibility.

### Decision
1. Every CRM record (`customers`, `leads`, `deals`) has `tenant_id` (mandatory UUID) and `branch_id` (nullable or mandatory UUID).
2. If `branch_id` is specified, the record belongs to that specific branch. Users with permissions limited to branch $B$ can only query and mutate records where `branch_id == B` (or where `branch_id IS NULL` for global tenant customers).
3. Corporate Customers can be shared across branches (global within tenant), while specific Deals are typically scoped to the operating Branch handling the commercial transaction.
4. Economic Groups and Customer Categories are tenant-level configuration (`tenant_id` only), accessible across all branches of the company.

---

## 5. Summary of Architecture Decisions

| Area | Decision | Rationale |
|---|---|---|
| **Domain Entities** | Separate clean aggregates (`Customer`, `Category`, `EconomicGroup`, `Lead`, `Pipeline`, `Deal`, `Activity`) | Replaces legacy 160-column flat table with high-cohesion, maintainable DDD entities |
| **Statutory Validation** | Strict Brazilian Modulo 11 for CPF (11 digits) and CNPJ (14 digits) | Guarantees zero invalid fiscal identifiers in database |
| **Document Storage** | Clean numeric digits with tenant composite unique constraint | High performance indexing, format-agnostic queries |
| **Lead Conversion** | Atomic transaction: `Lead.convert()` creates `Customer` and `Deal`, archives lead as `CONVERTED` | Prevents data duplication, eliminates human data re-entry |
| **Kanban Board** | Responsive columns with drag-and-drop, optimistic UI updates, and loss-reason modal | Fast (<50ms) user experience and complete commercial data capture |
| **Activity Timeline** | Polymorphic `CrmActivity` supporting `CUSTOMER`, `LEAD`, and `DEAL` | Unified chronological view across all sales touchpoints |
| **Custom Attributes** | Leverage existing dynamic `CustomFields` engine for target `CUSTOMER` | Eliminates hardcoding niche legacy fields (spouse, guarantors, etc.) |
