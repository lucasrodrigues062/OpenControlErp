# Research & Architectural Decisions: Dynamic Custom Fields Engine

**Feature**: `005-custom-fields`
**Date**: 2026-11-02
**Status**: Completed

## 1. Storage Architecture: Relational Definition with JSONB Values vs Pure EAV vs Dynamic DDL

### Decision
Adopt a **Hybrid Relational Definition + JSONB Document Architecture**:
1. **`custom_field_definitions` Table**: Fully relational metadata table storing field definitions (`id`, `tenant_id`, `target_entity`, `field_key`, `label`, `field_type`, `required`, `admin_only`, `read_only`, `display_order`, `options`, `validation_rules`, `is_active`, `created_at`, `updated_at`).
2. **`custom_field_values` Storage**: Entity instances store custom attributes as a JSONB map (e.g. `{"spouse_name": "Jane Doe", "vip_level": "PLATINUM"}`) or in a dedicated per-entity extension table `entity_custom_fields (tenant_id, target_entity, entity_id, field_values jsonb)`.

### Rationale
- **Pure EAV (Entity-Attribute-Value)** requires joining rows across tables for every entity fetch and makes sorting, filtering, and reporting excessively slow and cumbersome.
- **Dynamic DDL (altering database tables at runtime)** is hazardous in multi-tenant SaaS environments, incurs table locks, violates JPA entity mapping conventions, and poses database migration chaos.
- **PostgreSQL JSONB** allows indexable, lightning-fast key-value storage (`GIN` index support, `@>` containment queries), perfectly represents heterogeneous dynamic attributes per tenant, and eliminates table alterations when tenants create fields.
- Combining a relational definition table with JSONB value persistence gives strict metadata integrity, relational foreign keys, auditable configuration changes, and schema validation before any JSON write.

### Alternatives Considered
- *Pure EAV Table (`entity_id`, `field_id`, `string_value`, `num_value`)*: Rejected due to severe N+1 join overhead and complex reporting queries.
- *Runtime DDL Table Alters*: Rejected due to table locking, inability to safely rollback, and incompatibility with static JPA entity mappings.

---

## 2. Extensible Entity Registry Pattern

### Decision
Implement a **Decoupled Spring Bean Registry SPI (`CustomFieldEntityRegistry`)**:
- Target entities declare themselves in the registry via enum/descriptors (`CUSTOMER`, `SUPPLIER`, `PRODUCT`, `SALE_ORDER`, `FINANCIAL_TITLE`).
- Each registered entity descriptor provides:
  - Technical Identifier (e.g. `CUSTOMER`)
  - Human-friendly display label (translatable, e.g. "Customer", "Cliente")
  - Supported features (e.g., whether it supports rule evaluation hooks, search indexing, or export)
- Adding custom fields to a new module (e.g., when the Sales or Fiscal module is implemented) only requires registering its descriptor in the registry bean, without touching the custom fields kernel.

### Rationale
- Complies strictly with Constitution Principle I (Modular Monolith & Clean Architecture) and Principle II (Extensibility via Plugin Architecture). Modules remain autonomous.

### Alternatives Considered
- *Hardcoded Enum of Entities*: Rejected because it tightly couples the custom fields module to specific downstream domain entities.

---

## 3. Supported Field Types and Declarative Validation Engine

### Decision
Support 6 **Essential Field Types** with a centralized, typed `CustomFieldValidator`:
1. `TEXT`: String values with optional min/max length and regex pattern.
2. `NUMBER`: Integer numbers with optional min/max range.
3. `DECIMAL`: Floating-point / monetary numbers with scale precision.
4. `DATE`: ISO 8601 date string (`YYYY-MM-DD`).
5. `BOOLEAN`: Strict boolean (`true`/`false`).
6. `SELECT`: Single-select dropdown from an administrator-configured list of allowed options (strings).

### Rationale
- Meets the user's explicit selection of "Essential Types".
- Keeps validation deterministic, performant, and safe from arbitrary code injection.
- Serializes cleanly in JSON payloads.

---

## 4. Role-Based Field Flags & Access Control

### Decision
Implement **Field-Level Security Filtering** in the domain service / serializer:
- `admin_only` flag:
  - When evaluating read requests: If the authenticated user does NOT have the `ADMIN` / `SYSTEM_ADMIN` role or `CUSTOM_FIELDS_ADMIN` authority, `admin_only` fields are stripped from the response.
  - When evaluating write/mutation requests: If a non-admin attempts to submit or modify an `admin_only` field, the payload is rejected with a 403 Forbidden / validation violation.
- `read_only` flag:
  - On entity creation: Allowed to have initial value.
  - On entity update: If an existing entity record already contains a value for this field and the incoming update attempts to change it, the mutation is rejected with an immutability violation.

### Rationale
- Fulfills the user's selected choice for field flags without introducing the overhead of complex discretionary access control (DAC) ACL tables.

---

## 5. UI Architecture: Dynamic Component Rendering

### Decision
Create a reusable React component library in `frontend/src/components/customFields/`:
1. `CustomFieldFormSection.tsx`: Inspects target entity definitions, renders appropriate inputs (Text, Number, Datepicker, Select dropdown, Toggle switch), handles form validation errors, and respects `admin_only` and `read_only` states.
2. `CustomFieldDisplayCard.tsx`: Read-only summary card rendering labeled badges or key-value pairs for detail views.
3. `CustomFieldSettingsPage.tsx`: Administrative console with entity tabs, draggable/sortable order, type selection, options editor for `SELECT`, and preview.
4. Reusable API client `customFieldsApi.ts` with TanStack Query hooks.

### Rationale
- Enforces consistency across all future entity pages (Customers, Products, Suppliers) and eliminates code duplication.
