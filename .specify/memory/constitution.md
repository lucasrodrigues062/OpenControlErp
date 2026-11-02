<!--
SYNC IMPACT REPORT
==================
- Version change: 1.2.0 -> 1.3.0
- List of modified principles: None
- Added principles:
  * VIII. Mandatory Feature Documentation & Contract Transparency (NON-NEGOTIABLE)
- Added sections: None
- Removed sections: None
- Refinements:
  * Added mandatory quality gate requiring comprehensive Spec Kit artifacts, OpenAPI specs, module READMEs, and KDoc for all features prior to merge.
- Follow-up TODOs: None
-->

# OpenControl ERP Constitution

## Core Principles

### I. Modular Monolith & Clean Architecture
OpenControl ERP MUST be architected as a Modular Monolith with strict boundary enforcement (e.g., using Spring Modulith and Clean Architecture / Hexagonal principles). Modules (CRM, Inventory, Sales, Accounting, Financials, Fiscal) MUST maintain high cohesion and low coupling.
- Direct database cross-joins between module boundaries are prohibited; inter-module communication MUST occur via published domain interfaces or asynchronous domain events.
- Domain logic MUST remain independent of external frameworks, delivery mechanisms, and database persistence details.
- Distributed microservice complexity MUST be avoided until organizational or scalability demands empirically justify splitting specific boundaries.

### II. Extensibility via Plugin & App Architecture
The platform MUST provide a first-class Plugin/App extension architecture enabling developers and the community to add features without modifying the core kernel.
- Extension points MUST be defined as explicit Service Provider Interfaces (SPIs) and reactive domain event listeners.
- Plugins MUST have isolated lifecycles (install, enable, disable, update, uninstall) and clearly defined configuration manifests.
- Core business workflows MUST expose extension hooks before and after critical domain transitions (e.g., order validation, invoice generation, payment processing).

### III. Brazil-First Localization with Multi-Jurisdiction Abstraction
The system's initial implementation MUST focus deeply on Brazilian statutory, fiscal, and accounting requirements, while maintaining an architectural abstraction that enables future multi-country localization.
- Core business flows MUST interact with a generalized Fiscal and Tax Calculation SPI rather than hardcoded SEFAZ/Brazilian concepts.
- Brazilian fiscal compliance (NF-e, NFS-e, CT-e, MDF-e, SPED, etc.) MUST be implemented as a dedicated localization module/adapter, leveraging proven open-source Java DFe libraries (e.g., Samuel Oliveira's repositories).
- Internationalization (i18n) and multi-currency capabilities MUST be supported at the kernel level from day one.

### IV. Codebase Language & Jurisdiction Nomenclature Standards
The primary language of the codebase, APIs, database schemas, code comments, and technical documentation MUST be English.
- All domain entity names, services, and generic enterprise processes MUST use standard English (e.g., `Customer`, `Invoice`, `JournalEntry`, `TaxRate`, `InventoryMovement`).
- Jurisdiction-specific legal, fiscal, or regulatory identifiers that lack an unambiguous international translation MUST retain their official national names/acronyms (e.g., `NFe`, `NFSe`, `CTe`, `MDFe`, `SPED`, `CFOP`, `CST`, `ICMS`, `PIS`, `COFINS`, `registerNFE`).

### V. Modern Componentized UI with Design System Discipline
The user interface MUST be modern, accessible, performant, and built for maximum component reusability.
- The frontend stack MUST utilize TypeScript with a modern component-driven framework (React ecosystem with Next.js/Vite, Tailwind CSS, and headless/accessible UI primitives such as Radix UI / shadcn/ui).
- Core UI layouts MUST provide pluggable UI slots allowing installed Plugins/Apps to contribute navigational elements, dashboard widgets, and custom entity views seamlessly.
- State management and API data-fetching MUST be strongly typed and standardized (e.g., TanStack Query).

### VI. Transactional Integrity, Financial Immutability & Auditability
Financial and accounting records MUST follow strict double-entry bookkeeping rules and audit trail guarantees.
- General Ledger postings MUST be append-only and immutable; correction of erroneous postings MUST be performed via reversing entries (estorno), never direct in-place updates.
- All mutations to critical master data and transactional states MUST generate an immutable, timestamped audit log capturing the actor, operation, and state diff.

### VII. Minimum 90% Automated Test Coverage Gate (NON-NEGOTIABLE)
To guarantee system reliability, financial auditability, and error-free fiscal execution, all modules, services, and plugins MUST maintain a minimum of 90% automated test coverage (instruction, line, and branch coverage).
- Unit tests MUST cover all business calculation rules, state machine transitions, validations, and edge cases in isolation.
- Integration tests MUST verify database transactions, repository queries, SPI resolution, and event outbox delivery.
- The continuous integration (CI) pipeline and Gradle build (`jacocoTestCoverageVerification`) MUST fail immediately if test coverage falls below 90% on any module. PRs with coverage under 90% are strictly blocked from merging.

### VIII. Mandatory Feature Documentation & Contract Transparency (NON-NEGOTIABLE)
Every feature, business capability, architectural module, and plugin MUST include comprehensive, accurate, and up-to-date documentation before it can be merged or released. Undocumented code is considered incomplete and rejected by default.
- **Spec Kit Design Artifacts**: Every feature MUST possess its complete specification (`spec.md`), technical implementation plan (`plan.md`), entity data model (`data-model.md`), and runnable end-to-end guide (`quickstart.md`).
- **API & SPI Contracts**: All public REST endpoints MUST have complete OpenAPI (Swagger 3.0) descriptions including request/response schemas and error models. All SPI extension points MUST have clear contract definitions and lifecycle expectations.
- **Architecture & Operational Guides**: Every module MUST provide a `README.md` detailing its responsibilities, configuration properties, events produced/consumed, and extension points.
- **Code Documentation (KDoc/Javadoc)**: All public interfaces, classes, public methods, and non-trivial domain logic MUST be documented with descriptive docstrings.

## Technology Stack & Architecture Standards

### 1. Backend Core
- **Runtime & Language**: Kotlin 2.x targeting JVM 21 LTS (with 100% interoperability with Java libraries).
- **Framework**: Spring Boot 4.1.2 (or latest active LTS/stable line).
- **Build Tool**: Gradle (Kotlin DSL `build.gradle.kts`).
- **Modularity & Architecture**: Spring Modulith, Domain-Driven Design (DDD), Hexagonal Architecture.
- **Persistence & Database**: PostgreSQL as the primary relational database, managed through Spring Data JPA / Hibernate (with `kotlin-jpa` plugin) and versioned via Flyway database migrations.
- **Fiscal Engine Integration**: Decoupled fiscal integration layer integrating proven open-source Java libraries (e.g., `com.github.samuel-oliveira` DFe packages for SEFAZ communication).

### 2. Frontend Platform
- **Language & Runtime**: TypeScript (strict mode enabled).
- **Framework**: Modern React (Next.js / Vite SPA).
- **Styling & Components**: Tailwind CSS combined with shadcn/ui / Radix UI design tokens.
- **Data Layer**: OpenAPI/REST client generation with TanStack Query.

### 3. Architecture & Plugin Mechanism
- **Extension Points**: Spring Bean SPIs, Spring ApplicationEvent / transactional event listeners, and dynamic plugin loading.
- **Multi-Tenancy**: Architecture MUST support multi-company / multi-branch (filiais) data tenancy natively.

## Development Workflow, Quality Gates & Compliance

- **Test-Driven & Verification Quality**: All domain calculations (tax, financial, discount, inventory ledger) MUST have comprehensive unit test coverage. Fiscal integration adapters MUST include contract tests and mock SEFAZ service tests. Integration tests MUST use Testcontainers for realistic PostgreSQL testing.
- **Code Coverage Quality Gate**: JaCoCo is mandatory on every build. The build task `jacocoTestCoverageVerification` MUST enforce a minimum threshold of `0.90` (90%) for lines and branches.
- **Documentation Quality Gate**: Code reviews and CI checks MUST verify that corresponding documentation (`spec.md`, `quickstart.md`, OpenAPI annotations, module `README.md`, and KDoc comments) are present, accurate, and kept in sync with code modifications.
- **Static Analysis & Formatting**: Code formatting and style MUST be enforced via automated toolchains (e.g., Spotless ktlint, Checkstyle, SonarQube rules).
- **API Documentation**: All exposed REST endpoints MUST be defined with OpenAPI 3.0 / Swagger annotations.
- **Dependency Governance**: Any third-party dependency introduced into the core kernel MUST undergo review for security vulnerabilities, licensing compatibility (permissive open-source), and maintenance activity.

## Governance

- **Supremacy**: This Constitution defines the canonical architectural and engineering rules for OpenControl ERP. All technical designs, pull requests, and module specifications MUST comply with these principles.
- **Amendment Procedure**: Any modification or addition to this Constitution requires a formal proposal, justification, community/core maintainer consensus, and a migration plan for affected modules.
- **Versioning Policy**: The Constitution follows Semantic Versioning (`MAJOR.MINOR.PATCH`):
  - `MAJOR`: Fundamental architectural pivots or removal/breaking redefinition of core principles.
  - `MINOR`: Addition of new principles, new platform architectural tiers, or expanded governance rules.
  - `PATCH`: Wording clarifications, typo fixes, or non-semantic guideline refinements.
- **Compliance Verification**: All pull requests MUST undergo an architecture compliance check verifying adherence to modular boundaries, naming rules, test coverage gates (>= 90%), and mandatory documentation standards.

**Version**: 1.3.0 | **Ratified**: 2026-11-01 | **Last Amended**: 2026-11-01
