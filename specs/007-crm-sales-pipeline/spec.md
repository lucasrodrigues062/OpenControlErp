# Feature Specification: CRM, Customer Master Data, Leads & Sales Pipeline

**Feature Branch**: `007-crm-sales-pipeline`

**Created**: 2026-11-02

**Status**: Draft

**Input**: User description: "Vamos agora para o primeiro módulo do nosso sitema, pronto para o MVP. Preciso de um crm completo, com cadastro de clientes, categorias de clientes, grupos econimico, gestão de lead, pipeline. etc. tudo que um crm completo tem e precisa,"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Customer Master Data, Categorization & Economic Groups (Priority: P1) 🎯 MVP

As a Sales Operator or Commercial Manager,
I want to register, organize, and inspect customers as Individuals (Pessoa Física) or Legal Entities (Pessoa Jurídica), segment them by Customer Categories, and associate related companies under Economic Groups,
So that the commercial team has an accurate, statutory, and centralized 360-degree registry of all buyer relationships.

**Why this priority**: Customer master data is the core foundation of all commercial operations in an ERP. Without a registered customer, neither deals, sales orders, nor fiscal invoices can be created.

**Independent Test**: Register customer profiles with statutory Brazilian documents (CPF for individuals, CNPJ for companies with state registration), assign categories (e.g., Atacado, Varejo) and an economic group, and verify search, filtering, and credit limit indicators.

**Acceptance Scenarios**:

1. **Given** an authenticated user with customer management permissions, **When** they register a new Legal Entity customer with valid CNPJ, Trade Name, Legal Name, State Registration, and Address, **Then** the customer is persisted in the active tenant, formatted with official masking, and available for search.
2. **Given** a user inputting a CPF or CNPJ with invalid check digits (Modulo 11) or duplicate within the same tenant, **When** submitting the form, **Then** the system rejects the operation with an explicit validation error message.
3. **Given** multiple companies operating under the same corporate parent, **When** an Economic Group is created and linked to those customer accounts, **Then** the commercial dashboard reflects the consolidated group relationship and total credit exposure.
4. **Given** customer segmentation rules, **When** a Customer Category is assigned to a customer, **Then** the category's standard commercial terms (default payment condition, price table link) are reflected in their profile.

---

### User Story 2 - Lead Capture, Qualification & Conversion (Priority: P2)

As a Sales Representative or SDR (Sales Development Representative),
I want to capture inbound and outbound leads with contact details and acquisition sources, qualify their readiness, and convert qualified leads into customers with one click,
So that prospective buyers are systematically nurtured and moved without friction into active customer accounts.

**Why this priority**: Leads represent the top of the commercial funnel. Converting qualified leads directly into customers and sales opportunities prevents data re-entry and accelerates deal velocity.

**Independent Test**: Create a prospect lead from an acquisition source (e.g., Website, Inbound, Referral), progress their qualification status, and execute a qualification conversion to instantly produce an active customer account and an initial deal.

**Acceptance Scenarios**:

1. **Given** a new sales inquiry, **When** the salesperson creates a lead specifying origin source (e.g., Website, WhatsApp, Event, Referral), contact information, and interest summary, **Then** the lead is created in status "New" with assigned ownership.
2. **Given** a lead in qualification, **When** marked as "Unqualified", **Then** the user is required to record a rejection/disqualification reason for commercial analytics.
3. **Given** a lead validated as commercially viable, **When** the user clicks "Convert Lead", **Then** the system creates a new Customer entity, links existing lead interaction notes, creates an associated Opportunity/Deal in the sales pipeline, and marks the lead as "Converted".

---

### User Story 3 - Visual Sales Pipeline & Deal Opportunities (Priority: P3)

As a Salesperson or Commercial Director,
I want to manage opportunities through customizable sales funnel stages on an interactive Kanban board and list view, tracking estimated values, close dates, win/loss reasons, and stage progression,
So that the commercial pipeline is transparent, pipeline bottlenecks are resolved, and revenue forecasting is accurate.

**Why this priority**: Visual pipeline execution drives daily sales team behavior, shortens sales cycles, and provides real-time revenue projection.

**Independent Test**: Define custom pipeline stages (e.g., Qualification, Presentation, Negotiation, Won, Lost), create deals associated with customers, drag-and-drop deals across stages, and verify that total funnel values and win/loss analytics update in real time.

**Acceptance Scenarios**:

1. **Given** a sales pipeline with stages (e.g., Prospecting, Qualification, Proposal, Negotiation, Closed Won, Closed Lost), **When** a user drags a deal card from "Proposal" to "Negotiation", **Then** the deal stage updates instantly, recalculating funnel totals per stage.
2. **Given** an opportunity moved to "Closed Lost", **When** dropped into the lost stage, **Then** a modal prompts the user to select or describe the loss reason (e.g., Price, Competitor, Budget Canceled, Timing).
3. **Given** a pipeline dashboard view, **When** filtered by salesperson, date range, or economic group, **Then** total deal value, weighted pipeline value (based on stage probability), and average deal age reflect the selected filter.

---

### User Story 4 - Commercial Interactions, Timeline & Extension Integration (Priority: P4)

As a Sales Account Manager,
I want to log chronological activities (phone calls, emails, WhatsApp messages, meetings, notes) linked to customers, leads, and deals, and access dynamic custom fields and business rules,
So that complete relationship history is preserved across team handoffs and enterprise rules can validate commercial actions.

**Why this priority**: Preserves institutional memory across sales turnover and allows tenant-specific custom fields and automated rules (e.g., discount thresholds, credit limit checks) to extend CRM workflows.

**Independent Test**: Register a customer interaction (e.g., meeting notes, scheduled follow-up task), inspect the chronological timeline card, define a custom field for `CUSTOMER` confirming dynamic rendering, and trigger a business rule on customer update.

**Acceptance Scenarios**:

1. **Given** an active deal or customer profile, **When** a salesperson logs an interaction note or logs a call summary, **Then** the activity appears chronologically in the timeline with timestamp, author, and activity type.
2. **Given** tenant-defined custom fields registered for the `CUSTOMER` target entity, **When** creating or viewing a customer, **Then** dynamic custom field inputs and display sections render seamlessly in the customer detail view.
3. **Given** a custom business rule configured with trigger `CUSTOMER_CREDIT_CHECK`, **When** updating credit limits or closing deals, **Then** the rule engine evaluates criteria before persisting the transaction.

---

### Edge Cases

- **Duplicate Documents across Person Types**: An individual person (CPF) might later establish a legal entity (CNPJ). The system must enforce document uniqueness per document type, allowing the same contact email or phone across distinct legal entities while strictly rejecting duplicate CPFs or duplicate CNPJs within the same tenant.
- **Economic Group Cyclic Hierarchies**: An economic group cannot be set as a subgroup of itself or form circular reference dependencies.
- **Lead Conversion Concurrency**: If two sales reps attempt to convert the same lead simultaneously, the first transaction commits the conversion and the second receives a conflict message indicating the lead has already been converted.
- **Stage Progression Skipping**: Deals should be allowed to advance multiple stages at once (e.g., moving directly from Qualification to Closed Won for fast-track deals), while preserving stage entry timestamps for cycle duration metrics.
- **Deactivation with Open Opportunities**: When a customer is deactivated, active deals remain visible in the pipeline with a warning badge, preventing creation of new quotes until reactivated.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Customer Master Data & Segmentation
- **FR-001**: System MUST support customer registration for both Individuals (Pessoa Física with CPF) and Legal Entities (Pessoa Jurídica with CNPJ, State Registration, Municipal Registration, and Trade Name).
- **FR-002**: System MUST validate CPF and CNPJ documents using official Brazilian Modulo 11 check-digit verification algorithms and enforce uniqueness per tenant.
- **FR-003**: System MUST support Customer Categories with custom color badges, descriptions, and default commercial conditions (default payment terms, standard discount policies).
- **FR-004**: System MUST allow grouping multiple customer accounts under an Economic Group (Grupo Econômico) with holding name, CNPJ raiz consolidator, and consolidated credit exposure views.
- **FR-005**: System MUST provide statutory address management including CEP, street, number, complement, neighborhood, city, IBGE code, UF, and country.
- **FR-006**: System MUST track commercial credit parameters per customer (approved credit limit, credit status: Approved, Suspended, Blocked).

#### Lead Management
- **FR-007**: System MUST allow capturing leads with name, company name, email, phone/WhatsApp, interest description, estimated budget, and acquisition source (e.g., Website, Inbound, Event, Outbound, Referral, Social Media).
- **FR-008**: System MUST support lead lifecycle states: `NEW`, `IN_QUALIFICATION`, `QUALIFIED`, `DISQUALIFIED`, `CONVERTED`.
- **FR-009**: System MUST require a disqualification reason when a lead is transitioned to `DISQUALIFIED`.
- **FR-010**: System MUST provide a single-click conversion workflow that transitions a `QUALIFIED` lead into an active `Customer` and automatically initializes an `Opportunity` in the target sales pipeline.

#### Sales Pipeline & Opportunities (Deals)
- **FR-011**: System MUST support multiple configurable Sales Pipelines (e.g., Direct Sales, Inbound Sales, Enterprise Renewals) per tenant.
- **FR-012**: Each pipeline MUST consist of ordered stages with stage name, win probability percentage (0-100%), color indicator, and stage SLA warning threshold (days inactive).
- **FR-013**: System MUST allow creating opportunities/deals with title, linked customer/lead, estimated monetary value, currency, expected closing date, assigned salesperson (owner), and current stage.
- **FR-014**: System MUST provide both an interactive visual Kanban view (with drag-and-drop stage progression) and a paginated tabular view with filters (by owner, stage, date, economic group).
- **FR-015**: When an opportunity is moved to `CLOSED_LOST`, system MUST capture a standardized loss reason (Price, Lost to Competitor, Features/Scope, Project Postponed, Other).
- **FR-016**: System MUST calculate aggregate pipeline metrics per stage and overall (Total Pipeline Value, Weighted Expected Value, Win Rate percentage, Average Deal Velocity).

#### Interactions, Activities & Integration
- **FR-017**: System MUST provide an activity logging timeline supporting notes, phone calls, meetings, WhatsApp interactions, and scheduled follow-up tasks linked to customers, leads, or deals.
- **FR-018**: System MUST integrate with the dynamic Custom Fields engine for target entity `CUSTOMER`, enabling tenant-specific custom fields in customer forms and detail pages.
- **FR-019**: System MUST dispatch transactional domain events (`CUSTOMER_CREATED`, `CUSTOMER_UPDATED`, `LEAD_CONVERTED`, `DEAL_WON`, `DEAL_LOST`) through the outbox for audit logging and business rules execution.
- **FR-020**: System MUST enforce multi-branch context scoping, allowing commercial records to be associated with specific company branches or visible across all branches of the active tenant.

---

### Key Entities *(include if feature involves data)*

- **Customer (`Customer`)**: Core commercial master record representing an active or prospective buying party (Individual or Legal Entity). Attributes: `type` (INDIVIDUAL/LEGAL_ENTITY), `document` (CPF/CNPJ), `legalName`, `tradeName`, `stateRegistration`, `isExempt`, `status`, `creditLimit`, `categoryId`, `economicGroupId`, `address`, `contactInfo`.
- **CustomerCategory (`CustomerCategory`)**: Segmentation taxonomy grouping customers with shared commercial profiles. Attributes: `code`, `name`, `description`, `color`, `defaultPaymentTermId`.
- **EconomicGroup (`EconomicGroup`)**: Corporate holding conglomerate grouping related corporate customers. Attributes: `name`, `code`, `holdingCnpj`, `consolidatedCreditLimit`.
- **Lead (`Lead`)**: Prospective sales contact prior to qualification. Attributes: `name`, `companyName`, `email`, `phone`, `status` (`NEW`, `IN_QUALIFICATION`, `QUALIFIED`, `DISQUALIFIED`, `CONVERTED`), `source`, `assignedUserId`, `disqualificationReason`, `convertedCustomerId`, `convertedDealId`.
- **SalesPipeline (`SalesPipeline`)**: Configurable sales process funnel definition. Attributes: `name`, `code`, `isDefault`, `isActive`, `stages`.
- **PipelineStage (`PipelineStage`)**: Step within a sales pipeline. Attributes: `pipelineId`, `name`, `orderIndex`, `winProbability`, `color`, `maxDaysStagnant`.
- **Deal (`Deal`)**: Sales opportunity being negotiated. Attributes: `pipelineId`, `stageId`, `customerId`, `title`, `estimatedValue`, `expectedCloseDate`, `status` (`OPEN`, `WON`, `LOST`), `lossReason`, `assignedUserId`.
- **CrmActivity (`CrmActivity`)**: Chronological interaction or task. Attributes: `entityType` (CUSTOMER/LEAD/DEAL), `entityId`, `activityType` (NOTE, CALL, MEETING, WHATSAPP, TASK), `summary`, `description`, `performedAt`, `authorUserId`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Sales operators can register a complete customer profile with CNPJ/CPF validation and address in under **90 seconds** (with CEP auto-fill assistance).
- **SC-002**: Lead conversion into an active Customer and Opportunity completes in **1 click** with transaction latency under **250ms**.
- **SC-003**: The Kanban board smoothly renders and updates across stage transitions with drag-and-drop response under **50ms** for pipelines with up to 500 active deals.
- **SC-004**: Zero invalid CPFs or CNPJs persisted in the database, guaranteed by strict Modulo 11 check-digit verification.
- **SC-005**: 100% of deal stage changes and customer status updates generate an immutable entry in the audit trail.
- **SC-006**: Complete internationalization support for English (`en-US`) and Brazilian Portuguese (`pt-BR`) across all CRM labels, filters, and pipeline views.
- **SC-007**: Automated test coverage achieves $\ge 90\%$ line and branch coverage across all CRM controllers, domain models, services, and repositories per Constitution Principle VII.

---

## Assumptions

- **Statutory Document Masking**: Brazilian users expect standard format rendering (`000.000.000-00` for CPF, `00.000.000/0000-00` for CNPJ, `00000-000` for CEP) while clean unmasked numeric digits are stored in the database for indexing efficiency.
- **Currency & Local Time**: Monetary values in CRM deals default to BRL (R$) with multi-currency readiness at the data model level. Timestamps are captured in UTC and rendered in user browser local timezone.
- **Security & Scoping**: Commercial operators can view all deals in their assigned company branch; commercial managers/administrators have cross-branch visibility across the active tenant.
- **No Third-Party CRM Dependency**: The module operates natively inside OpenControl ERP without requiring external CRM SaaS tools (e.g., Salesforce or HubSpot), preserving data sovereignty and zero recurring integration overhead.
