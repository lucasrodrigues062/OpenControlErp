# Implementation Plan: Dynamic Custom Fields Engine for Business Entities

**Branch**: `main` | **Date**: 2026-11-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/005-custom-fields/spec.md`

## Summary

Deliver a tenant-isolated, dynamic custom fields subsystem that allows companies to define arbitrary typed attributes (Text, Number, Decimal, Date, Boolean, Select dropdown) for any business entity registered in an extensible registry (starting with Customer, Supplier, and Product). The feature includes declarative validations, role-based field flags (`adminOnly`, `readOnly`), JSONB value persistence, and reusable React form and card components.

## Technical Context

**Language/Version**: Kotlin 2.1 (JVM 21) & TypeScript 5.6 (strict mode)  
**Primary Dependencies**: Spring Boot 4.1.2, Spring Modulith, Spring Data JPA, Hibernate, PostgreSQL JSONB, React 18, Tailwind CSS, Lucide React, TanStack Query  
**Storage**: PostgreSQL relational metadata (`custom_field_definitions`) with JSONB document storage (`entity_custom_fields`)  
**Testing**: JUnit 5, MockK, Spring Boot Test, Testcontainers, Vitest, Testing Library  
**Target Platform**: Web application (REST API + React SPA)  
**Project Type**: Modular Monolith Full-Stack Feature  
**Performance Goals**: Entity custom field resolution and validation in < 15ms; form rendering under 200ms for up to 50 active fields  
**Constraints**: Zero cross-tenant data leakage; strict backward-compatibility preserving historical records on field deactivation; $\ge 90\%$ JaCoCo test coverage  
**Scale/Scope**: Multi-tenant ERP; support hundreds of custom field definitions per tenant across arbitrary entity types  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: Modular Monolith & Clean Architecture**: PASS. Implemented as an autonomous module `org.opencontrol.erp.customfields` with distinct domain, service, repository, and web layers.
- **Principle II: Extensibility via Plugin & App Architecture**: PASS. Decoupled using an extensible `CustomFieldEntityRegistry` SPI allowing any module or plugin to register new target entity types without core schema alteration.
- **Principle III: Brazil-First Localization & Multi-Currency**: PASS. All field types (e.g. Decimal, Date) respect locale and standard formatting.
- **Principle IV: Codebase Language & Standards**: PASS. 100% English code, entity names, API endpoints, and technical documentation.
- **Principle V: Modern Componentized UI with Design System**: PASS. Reusable `CustomFieldFormSection` and `CustomFieldDisplayCard` built with React, Tailwind CSS, and Lucide icons.
- **Principle VI: Transactional Integrity & Auditability**: PASS. All custom field mutations occur within tenant transaction boundaries and maintain audit timestamps.
- **Principle VII: Minimum 90% Automated Test Coverage Gate**: PASS. Comprehensive unit and integration test suite targeting $\ge 90\%$ coverage for all new services and controllers.
- **Principle VIII: Mandatory Feature Documentation & Contract Transparency**: PASS. Complete Spec Kit artifacts generated: `spec.md`, `research.md`, `data-model.md`, `contracts/custom-fields-api.yaml`, `quickstart.md`, and `plan.md`.

## Project Structure

### Documentation (this feature)

```text
specs/005-custom-fields/
├── plan.md              # Implementation plan (/speckit-plan)
├── research.md          # Architectural decisions & research (/speckit-plan)
├── data-model.md        # Relational & JSONB data model (/speckit-plan)
├── quickstart.md        # Runnable end-to-end verification guide (/speckit-plan)
├── contracts/
│   └── custom-fields-api.yaml # OpenAPI 3.0 contract specification
├── checklists/
│   └── requirements.md  # Quality verification checklist
└── tasks.md             # Implementation tasks (/speckit-tasks)
```

### Source Code

```text
backend/
├── src/main/kotlin/org/opencontrol/erp/customfields/
│   ├── api/
│   │   ├── CustomFieldController.kt
│   │   └── dto/
│   │       └── CustomFieldDtos.kt
│   ├── domain/
│   │   ├── CustomFieldDefinition.kt
│   │   ├── EntityCustomFields.kt
│   │   └── CustomFieldEnums.kt
│   ├── repository/
│   │   ├── CustomFieldDefinitionRepository.kt
│   │   └── EntityCustomFieldsRepository.kt
│   ├── service/
│   │   ├── CustomFieldDefinitionService.kt
│   │   ├── CustomFieldValueService.kt
│   │   ├── CustomFieldValidator.kt
│   │   └── CustomFieldEntityRegistry.kt
│   └── exception/
│       └── CustomFieldExceptions.kt
├── src/main/resources/db/migration/
│   └── V1_0_8__create_custom_fields_tables.sql
└── src/test/kotlin/org/opencontrol/erp/customfields/
    ├── api/CustomFieldControllerTest.kt
    ├── service/CustomFieldDefinitionServiceTest.kt
    ├── service/CustomFieldValueServiceTest.kt
    ├── service/CustomFieldValidatorTest.kt
    └── QuickstartCustomFieldsEndToEndTest.kt

frontend/
├── src/
│   ├── api/
│   │   └── customFieldsApi.ts
│   ├── components/customFields/
│   │   ├── CustomFieldFormSection.tsx
│   │   ├── CustomFieldDisplayCard.tsx
│   │   └── CustomFieldDefinitionModal.tsx
│   ├── pages/
│   │   └── CustomFieldsSettingsPage.tsx
│   ├── types/
│   │   └── customFields.ts
│   └── locales/
│       ├── en-US/customFields.ts
│       └── pt-BR/customFields.ts
```

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|:----------|:-----------|:-------------------------------------|
| None | N/A | Fully adheres to standard architecture and patterns. |
