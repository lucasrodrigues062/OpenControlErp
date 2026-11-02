# Feature Specification: Dynamic Custom Fields Engine for Business Entities

**Feature Branch**: `005-custom-fields`

**Created**: 2026-11-02

**Status**: Draft

**Input**: User description: "Antes de começarmos a cadastras as entidades principais. Produtos, clientes fornecedores etc... Imagina que cada negócio tem suas particularidades, sendo assim fica impossivel criar todos os campos necessários para uma entidade, por exemplo uma empresa pode querer o nome do conjugue do cliente, e outra empresa não precisa. A ideia é dar a opção de criar campos adicionais livres, para as entendidades conforme cada empresa tenha necessidade"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Define Custom Fields per Business Entity (Priority: P1)

As a Tenant Administrator or Business Manager, I want to define custom fields for standard entities (such as Customers, Suppliers, and Products) with a designated label, field key, data type, and validation rules (e.g., required, options list), so that our ERP instance captures specific domain data unique to our industry and business process without modifying the core software.

**Why this priority**: Without the ability to define custom field schemas dynamically, companies cannot adapt standard entities to their operational nuances before creating master records.

**Independent Test**: Can be tested by creating a custom field definition (e.g., "Spouse Name" on Customer entity, or "Batch Expiration Shelf" on Product) and verifying that the field schema is persisted, listed in the configuration catalog, and made available for data entry.

**Acceptance Scenarios**:

1. **Given** an authenticated administrator in tenant "Acme Corp", **When** they create a custom field with key `spouse_name`, label "Spouse Name", type "Text", and target entity "Customer", **Then** the custom field is saved and appears in the Customer custom field configuration list.
2. **Given** an administrator configuring a field, **When** they set the field as "Required" with a dropdown type and define options ["Retail", "Wholesale", "Key Account"], **Then** the system validates that options are non-empty and stores the validation rules.
3. **Given** an administrator attempting to create a custom field with a key that already exists for the same target entity in their tenant, **When** they submit, **Then** the system rejects the duplicate key with a clear validation message.

---

### User Story 2 - Fill and Validate Custom Fields on Entity Records (Priority: P1)

As an ERP Operator or Salesperson, I want to fill in values for custom fields when creating or editing an entity (such as entering a customer's spouse name, or a supplier's tax regime exemption code), and have the system enforce data types and validation rules, so that business records maintain data integrity and completeness.

**Why this priority**: Custom field definitions are only valuable when operators can seamlessly populate and view them during entity lifecycles.

**Independent Test**: Can be tested by creating a new entity instance (e.g., Customer) with custom field values, saving it, and verifying that retrieving the customer record returns the populated custom fields matching their schema types.

**Acceptance Scenarios**:

1. **Given** an active required custom field `spouse_name` for Customer, **When** an operator submits a new Customer without filling `spouse_name`, **Then** the system prevents submission and highlights the missing required field.
2. **Given** a custom field of type "Date", **When** an operator submits an invalid date format, **Then** the system rejects the payload with a type format error.
3. **Given** a saved entity with custom field values, **When** the entity details view is rendered, **Then** all custom fields and their current values are displayed accurately alongside standard fields.

---

### User Story 3 - Manage Lifecycle and Reordering of Custom Fields (Priority: P2)

As an Administrator, I want to edit field labels, reorder their presentation position, activate/deactivate fields, or archive obsolete fields without losing existing historical entity data, so that our entity forms remain clean and adapt to changing company policies over time.

**Why this priority**: Businesses evolve continuously; fields become obsolete or require renaming without breaking historical records.

**Independent Test**: Can be tested by deactivating an existing custom field, confirming it no longer appears on new entity creation forms, while previously saved records retain their historical values.

**Acceptance Scenarios**:

1. **Given** an existing custom field, **When** an administrator toggles its status to "Inactive", **Then** the field is hidden from new entity creation and edit forms, but past entity records continue displaying historical values.
2. **Given** multiple custom fields for an entity, **When** an administrator adjusts their display sequence order, **Then** entity forms render the fields in the newly specified order.
3. **Given** a custom field with existing values across records, **When** an administrator attempts to delete the field definition, **Then** the system prompts for soft-archival or safely archives it to prevent catastrophic data loss.

---

### User Story 4 - Consume Custom Fields in Business Rules and Automations (Priority: P3)

As a Business Rule Creator, I want custom field values to be accessible in the Custom Rules Engine condition builder and evaluation context, so that dynamic policies (e.g., "If customer.custom_fields.spouse_name is empty, notify sales rep") can trigger business workflows seamlessly.

**Why this priority**: Integrates custom fields into the broader platform extensibility ecosystem established in previous features.

**Independent Test**: Can be tested by creating a custom rule that inspects a custom field value and verifying that the rule correctly fires or halts during entity lifecycle events.

**Acceptance Scenarios**:

1. **Given** a custom field `credit_score_bracket` on Customer, **When** a business rule evaluates condition `#customer.customFields['credit_score_bracket'] == 'HIGH'`, **Then** the evaluation engine resolves the custom field value correctly.

---

### Edge Cases

- **Duplicate Field Keys**: Prevent two fields on the same entity type within the same tenant from using identical keys, while allowing identical keys across different entity types (e.g., `observation` on both Customer and Product).
- **Type Alteration Safeguards**: If a field already has stored values across records, prevent changing its core type from String to Number/Date in a manner that would corrupt existing data.
- **Tenant Isolation**: Custom field definitions and values must never leak or be visible across distinct tenant boundaries.
- **Deleted/Inactive Fields on Existing Entities**: When viewing historical records containing values for deactivated fields, the system should display them in read-only mode with an indicator that the field definition is inactive.
- **Empty or Null Values on Optional Fields**: System must cleanly distinguish between empty string, null, and non-configured fields without errors.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow tenant administrators to define custom fields bound to any business entity type registered in the ERP's Extensible Entity Registry (initially supporting Customer, Supplier, and Product, while architected so any future core module or plugin can register its target entity type without altering the core schema).
- **FR-002**: System MUST support essential field data types (Text, Number, Decimal, Date, Boolean, and Single-Select Dropdown with configurable predefined options) with type-specific validation rules (required flag, numeric minimum/maximum bounds, default values, and valid choice enforcement).
- **FR-003**: System MUST support role-based field configuration flags, allowing administrators to designate individual custom fields as "Admin Only" (viewable/editable only by users with administrative privileges) or "Read-Only" (immutable after initial entity creation).
- **FR-004**: System MUST strictly isolate all custom field definitions and stored entity values by tenant ID (`tenant_id`).
- **FR-005**: System MUST enforce unique field keys per target entity type within a tenant.
- **FR-006**: System MUST validate custom field values against their definition rules (data type, required status, allowed options list, numeric bounds) during entity creation and update.
- **FR-007**: System MUST allow editing field metadata (label, description, display order, required flag, active status) without destroying historical records.
- **FR-008**: System MUST provide a standardized API and UI component pattern so that future core modules (Sales, CRM, Catalog, Inventory) can embed custom field forms and display cards without redundant code.
- **FR-009**: System MUST support soft-deactivation of custom field definitions, preserving stored data integrity for historical audit compliance.
- **FR-010**: System MUST expose custom field values in entity payloads so that external systems, reports, and the Custom Rules Engine can access them cleanly.

### Key Entities *(include if feature involves data)*

- **CustomFieldDefinition**: Represents the schema/metadata of a custom attribute created by a tenant administrator. Key attributes: target entity type (e.g., `CUSTOMER`, `SUPPLIER`, `PRODUCT`), field key/code, display label, description, data type, validation configuration (required, min/max length, regex, choices/options), display order, status (active/inactive), and tenant identifier.
- **CustomFieldValue**: Represents the concrete value assigned to a custom field for a specific entity record. Key attributes: target entity type, entity identifier, custom field definition reference, typed value representation, and tenant identifier.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Administrators can configure and publish a new custom field for an entity in under 60 seconds without technical support or code changes.
- **SC-002**: Operators can input and save custom field values during entity registration with 100% schema validation accuracy.
- **SC-003**: 100% of custom field definitions and values are strictly partitioned by tenant ID, with zero cross-tenant visibility.
- **SC-004**: System handles entity forms with up to 50 active custom fields per entity with no discernible rendering delay (form render under 200ms).
- **SC-005**: 0% data corruption or loss when custom fields are renamed, reordered, or deactivated over time.

## Assumptions

- Core entity masters (Customer, Supplier, Product) will adopt this custom fields component as their standardized attribute extension mechanism.
- Custom field keys will follow alphanumeric snake_case conventions (e.g., `spouse_name`, `delivery_window_start`) for programmatic consistency and rule engine integration.
- Custom field values can be stored in an optimized, structured format (such as an EAV or JSONB attribute model with relational definition integrity) aligned with PostgreSQL best practices.
- The UI will dynamically generate appropriate form controls (text input, number input, date picker, checkbox, select dropdown) based on the field definition's data type.
