# Implementation Plan: Extension Engine & Plugin Architecture

**Branch**: `001-extension-engine` | **Date**: 2026-11-01 | **Spec**: [specs/001-extension-engine/spec.md](spec.md)

**Input**: Feature specification from `/specs/001-extension-engine/spec.md`

## Summary

Implement the foundational Extension Engine for OpenControl ERP, providing a dynamic Plugin Lifecycle manager, a strongly-typed Service Provider Interface (SPI) registry with contextual resolution, and a dual-phase Event Bus (synchronous pre-commit validation interceptors and asynchronous post-commit transactional outbox listeners). This delivers the core architectural mechanism needed to support modular Apps (including the Brazilian Fiscal Engine) without modifying kernel source code.

## Technical Context

**Language/Version**: Kotlin 2.x (targeting JVM 21 LTS)  
**Primary Dependencies**: Spring Boot 4.1.2 (Spring Framework 7.x line), Spring Modulith, Spring Data JPA (`kotlin-spring`, `kotlin-jpa`), Flyway Database Migrations, Gradle (Kotlin DSL)  
**Storage**: PostgreSQL 16+ (Relational tables for plugin registry, dependency graph, audit logging, and transactional event outbox)  
**Testing**: JUnit 5, AssertJ, MockK / Mockito, Testcontainers (PostgreSQL container integration tests)  
**Target Platform**: Linux / Windows containerized JVM environments  
**Project Type**: Modular Monolith Core Kernel / Platform Extension Runtime  
**Performance Goals**: SPI contextual resolution overhead < 5ms; Event outbox throughput > 1,000 events/sec; Plugin activation < 10s without JVM restart  
**Constraints**: Zero downtime during plugin state transitions; Asynchronous subscriber failures must never abort originating business transactions (fault isolation)  
**Scale/Scope**: Support 50+ concurrent active extensions and continuous event streaming with transactional consistency  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: Modular Monolith & Clean Architecture**: PASS. The extension engine resides in its own isolated kernel module (`org.opencontrol.erp.kernel.plugin`), with no direct dependency on concrete business domains (Sales, CRM, Finance).
- **Principle II: Extensibility via Plugin & App Architecture**: PASS. This implementation directly fulfills Principle II, establishing the formal SPI registry, manifest descriptor, and event bus.
- **Principle III: Brazil-First Localization with Multi-Jurisdiction Abstraction**: PASS. The engine provides the generic `FiscalEngineSPI` and contextual matcher so Brazilian DFe compliance can plug in without polluting core invoicing flows.
- **Principle IV: Codebase Language & Jurisdiction Nomenclature Standards**: PASS. All kernel classes, interfaces, APIs, tables, and documentation are strictly authored in English.
- **Principle V: Modern Componentized UI with Design System Discipline**: PASS. Extension management endpoints expose manifest metadata and capability descriptors for frontend consumption.
- **Principle VI: Transactional Integrity, Financial Immutability & Auditability**: PASS. Uses PostgreSQL Transactional Outbox pattern for event durability and an immutable `extension_audit_log` table for all administrative state transitions.
- **Principle VII: Minimum 90% Automated Test Coverage Gate (NON-NEGOTIABLE)**: PASS. JaCoCo configured with `jacocoTestCoverageVerification` enforcing a minimum `0.90` (90%) limit. All kernel components, services, algorithms, and controllers are accompanied by thorough unit and integration test suites.
- **Principle VIII: Mandatory Feature Documentation & Contract Transparency (NON-NEGOTIABLE)**: PASS. Feature includes complete `spec.md`, `plan.md`, `data-model.md`, `quickstart.md`, OpenAPI specification (`contracts/extension-management-api.yaml`), JSON Schema manifest (`contracts/plugin-manifest-schema.json`), SPI contracts guide (`contracts/spi-contracts.md`), and module architectural `backend/README.md`.

## Project Structure

### Documentation (this feature)

```text
specs/001-extension-engine/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   ├── extension-management-api.yaml
│   ├── plugin-manifest-schema.json
│   └── spi-contracts.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
└── src/
    ├── main/
    │   ├── kotlin/org/opencontrol/erp/kernel/
    │   │   ├── plugin/
    │   │   │   ├── domain/               # ExtensionPlugin, Manifest, PluginStatus, StateMachine
    │   │   │   ├── loader/               # Isolated ClassLoader, ManifestParser, PackageValidator
    │   │   │   ├── service/              # PluginLifecycleService, DependencyResolver
    │   │   │   └── repository/           # Spring Data JPA repositories & AuditLog
    │   │   ├── spi/
    │   │   │   ├── ExtensionPoint.kt     # Marker interface
    │   │   │   ├── ContextMatcher.kt     # Context evaluation contract
    │   │   │   ├── ExtensionRegistry.kt  # Centralized SPI registry
    │   │   │   └── ExecutionContext.kt   # Multi-tenant & jurisdiction context
    │   │   ├── event/
    │   │   │   ├── DomainEvent.kt        # Base immutable event record
    │   │   │   ├── DomainEventPublisher.kt # Pre-commit & Outbox publisher
    │   │   │   ├── outbox/               # EventOutbox entity, poller, retry worker
    │   │   │   └── listener/             # Annotation scanners & dispatchers
    │   │   └── api/
    │   │       ├── ExtensionController.kt
    │   │       └── dto/
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │           ├── V1_0_0__create_extension_schema.sql
    │           └── V1_0_1__create_event_outbox_schema.sql
    └── test/
        ├── kotlin/org/opencontrol/erp/kernel/
        │   ├── plugin/                   # Unit & Lifecycle state machine tests
        │   ├── spi/                      # SPI resolution & fallback tests
        │   └── event/                    # Pre-commit & Outbox integration tests
        └── resources/
            └── test-plugins/             # Sample mocked plugin JAR archives
```

**Structure Decision**: Standard Gradle multi-project / clean package layout adhering to Spring Modulith conventions under `backend/`. The kernel is isolated under `org.opencontrol.erp.kernel`, establishing contracts that downstream modules (`fiscal`, `sales`, `crm`) will implement.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*(No constitution violations. Architecture is fully aligned with all constitutional principles.)*
