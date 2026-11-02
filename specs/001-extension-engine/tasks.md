# Tasks: Extension and Plugin Engine

**Input**: Design documents from `/specs/001-extension-engine/`  
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md), [research.md](research.md)  
**Tests**: Unit and integration test tasks are included per the project quality standards and constitution.

## Format: `- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Explicit file paths included in all tasks

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, build setup, and baseline tooling configuration.

- [x] T001 Initialize Kotlin 2.x on JVM 21 with Spring Boot 4.1.2 Gradle project (`kotlin.jvm`, `kotlin.spring`, `kotlin.jpa`), Spring Modulith, Spring Data JPA, Validation, and PostgreSQL driver in backend/build.gradle.kts and backend/settings.gradle.kts
- [x] T002 [P] Configure Spotless Gradle plugin for Kotlin (ktlint) and code formatting rules in backend/build.gradle.kts
- [x] T003 [P] Configure base Spring Boot application profile and PostgreSQL datasource settings in backend/src/main/resources/application.yml
- [x] T004 [P] Configure Testcontainers and JUnit 5 base integration test configuration in backend/src/test/kotlin/org/opencontrol/erp/kernel/BaseIntegrationTest.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T005 Setup Flyway database migration baseline for the kernel schema in backend/src/main/resources/db/migration/V1_0_0__init_kernel_schema.sql
- [x] T006 [P] Create base exception hierarchy and global REST error handler in backend/src/main/kotlin/org/opencontrol/erp/kernel/exception/GlobalExceptionHandler.kt
- [x] T007 [P] Create base immutable domain event record interface in backend/src/main/kotlin/org/opencontrol/erp/kernel/event/DomainEvent.kt
- [x] T008 [P] Implement security and tenant audit context holder in backend/src/main/kotlin/org/opencontrol/erp/kernel/context/SecurityContextHolder.kt

**Checkpoint**: Foundation ready - user story implementation can now begin.

---

## Phase 3: User Story 1 - App Lifecycle Management (Priority: P1) 🎯 MVP

**Goal**: Enable administrators to discover, upload, validate, install, activate, deactivate, and uninstall modular extensions (Apps) dynamically without JVM restart.

**Independent Test**: Upload a valid plugin JAR package via REST API, verify state transitions (`INSTALLED` ➔ `ACTIVE` ➔ `INACTIVE` ➔ uninstalled), and verify audit log entries without restarting the application.

### Tests for User Story 1
- [x] T009 [P] [US1] Unit test for manifest parser and SemVer validation in backend/src/test/kotlin/org/opencontrol/erp/kernel/plugin/ManifestParserTest.kt
- [x] T010 [P] [US1] Unit test for DAG topological sort and circular dependency detection in backend/src/test/kotlin/org/opencontrol/erp/kernel/plugin/DependencyResolverTest.kt
- [x] T011 [P] [US1] Integration test for extension lifecycle transitions and audit logging in backend/src/test/kotlin/org/opencontrol/erp/kernel/plugin/ExtensionLifecycleIntegrationTest.kt

### Implementation for User Story 1
- [x] T012 [P] [US1] Create Flyway migration for `extension_plugin`, `extension_dependency`, and `extension_audit_log` tables with status constraint `VARCHAR(30) NOT NULL` in backend/src/main/resources/db/migration/V1_0_1__create_extension_lifecycle_tables.sql
- [x] T013 [P] [US1] Create JPA entity `ExtensionPlugin` with status enum `DISCOVERED, INSTALLED, ACTIVE, INACTIVE, ERROR` in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/domain/ExtensionPlugin.kt
- [x] T014 [P] [US1] Create JPA entity `ExtensionDependency` with `version_range` and `optional` fields in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/domain/ExtensionDependency.kt
- [x] T015 [P] [US1] Create JPA entity `ExtensionAuditLog` with immutable action logging in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/domain/ExtensionAuditLog.kt
- [x] T016 [P] [US1] Create Spring Data JPA repositories for extension entities in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/repository/ExtensionPluginRepository.kt
- [x] T017 [US1] Implement JSON manifest parser and schema validator for `META-INF/opencontrol-plugin.json` in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/loader/ManifestParser.kt
- [x] T018 [US1] Implement isolated child classloader `PluginClassLoader` for dynamic archive isolation in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/loader/PluginClassLoader.kt
- [x] T019 [US1] Implement DAG dependency resolver and circular dependency detector using Kahn's algorithm in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/service/DependencyResolver.kt
- [x] T020 [US1] Implement `PluginLifecycleService` orchestrating install, activate, deactivate, and uninstall transitions with state machine checks in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/service/PluginLifecycleService.kt
- [x] T021 [US1] Implement `ExtensionController` and DTOs per OpenAPI contract in backend/src/main/kotlin/org/opencontrol/erp/kernel/api/ExtensionController.kt

**Checkpoint**: User Story 1 is fully functional and delivers an independently testable MVP.

---

## Phase 4: User Story 2 - Service Provider Interface (SPI) Contract Extensibility (Priority: P2)

**Goal**: Allow core modules and active plugins to provide and consume strongly-typed extension points (SPIs) with contextual priority resolution and default fallbacks.

**Independent Test**: Register two mock SPI providers with different priorities, verify that the engine resolves the highest-priority provider matching the execution context (e.g., `country="BR"`), and verify fallback to default implementation when no plugin is active.

### Tests for User Story 2
- [x] T022 [P] [US2] Unit test for SPI registry priority ordering and contextual matching in backend/src/test/kotlin/org/opencontrol/erp/kernel/spi/ExtensionRegistryTest.kt
- [x] T023 [P] [US2] Integration test for dynamic SPI provider registration, unregistration on plugin deactivation, and default fallback in backend/src/test/kotlin/org/opencontrol/erp/kernel/spi/ExtensionRegistryIntegrationTest.kt

### Implementation for User Story 2
- [x] T024 [P] [US2] Create Flyway migration for `extension_point_registration` table in backend/src/main/resources/db/migration/V1_0_2__create_extension_point_tables.sql
- [x] T025 [P] [US2] Create core SPI marker interface `ExtensionPoint` and `ContextMatcher` in backend/src/main/kotlin/org/opencontrol/erp/kernel/spi/ExtensionPoint.kt
- [x] T026 [P] [US2] Create `ExecutionContext` data class capturing tenant, jurisdiction, and channel properties in backend/src/main/kotlin/org/opencontrol/erp/kernel/spi/ExecutionContext.kt
- [x] T027 [P] [US2] Create JPA entity `ExtensionPointRegistration` in backend/src/main/kotlin/org/opencontrol/erp/kernel/spi/domain/ExtensionPointRegistration.kt
- [x] T028 [US2] Implement centralized `ExtensionRegistry` with priority weighting and contextual filtering in backend/src/main/kotlin/org/opencontrol/erp/kernel/spi/service/ExtensionRegistry.kt
- [x] T029 [US2] Create sample generic SPI `FiscalEngineSPI` and `DefaultNoOpFiscalEngine` in backend/src/main/kotlin/org/opencontrol/erp/kernel/spi/fiscal/FiscalEngineSPI.kt
- [x] T030 [US2] Connect `PluginLifecycleService` to automatically register SPI providers on plugin activation and unbind on deactivation in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/service/PluginLifecycleService.kt

**Checkpoint**: User Stories 1 and 2 work seamlessly together, allowing pluggable SPIs to be bound to active plugins.

---

## Phase 5: User Story 3 - Unified Domain Event Bus (Priority: P3)

**Goal**: Provide a reliable dual-phase event pipeline supporting synchronous pre-commit validation hooks and asynchronous post-commit transactional outbox listeners.

**Independent Test**: Verify that a pre-commit interceptor can reject a transaction with a validation error, and verify that post-commit listeners receive events asynchronously via PostgreSQL outbox table with automatic retry on failure.

### Tests for User Story 3
- [x] T031 [P] [US3] Unit test for pre-commit interceptor pipeline aborting transaction on validation exception in backend/src/test/kotlin/org/opencontrol/erp/kernel/event/PreCommitInterceptorTest.kt
- [x] T032 [P] [US3] Integration test for post-commit outbox persistence, background dispatch, and exponential backoff retry in backend/src/test/kotlin/org/opencontrol/erp/kernel/event/EventOutboxIntegrationTest.kt

### Implementation for User Story 3
- [x] T033 [P] [US3] Create Flyway migration for `event_outbox` and `event_delivery_attempt` tables in backend/src/main/resources/db/migration/V1_0_3__create_event_outbox_tables.sql
- [x] T034 [P] [US3] Create JPA entity `EventOutbox` with status `PENDING, PROCESSING, PUBLISHED, FAILED, DEAD_LETTER` in backend/src/main/kotlin/org/opencontrol/erp/kernel/event/domain/EventOutbox.kt
- [x] T035 [P] [US3] Create JPA entity `EventDeliveryAttempt` in backend/src/main/kotlin/org/opencontrol/erp/kernel/event/domain/EventDeliveryAttempt.kt
- [x] T036 [P] [US3] Create Spring Data JPA repository for `EventOutbox` with locked batch queries in backend/src/main/kotlin/org/opencontrol/erp/kernel/event/repository/EventOutboxRepository.kt
- [x] T037 [US3] Implement `DomainEventPublisher` with synchronous pre-commit interceptors and transactional outbox persistence in backend/src/main/kotlin/org/opencontrol/erp/kernel/event/service/DomainEventPublisher.kt
- [x] T038 [US3] Implement background outbox worker utilizing Java 21 Virtual Threads / Coroutines and exponential backoff retry in backend/src/main/kotlin/org/opencontrol/erp/kernel/event/worker/OutboxDispatchWorker.kt
- [x] T039 [US3] Implement dead-letter manual retry endpoint in `EventOutboxController` in backend/src/main/kotlin/org/opencontrol/erp/kernel/api/EventOutboxController.kt

**Checkpoint**: All three user stories are functional and independently verifiable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: System verification, API documentation validation, and operational hardening.

- [x] T040 [P] Execute end-to-end scenarios documented in quickstart.md using an automated test harness in backend/src/test/kotlin/org/opencontrol/erp/kernel/QuickstartEndToEndTest.kt
- [x] T041 [P] Configure OpenAPI documentation generator (SpringDoc OpenAPI 3.0) and verify Swagger UI availability in backend/src/main/kotlin/org/opencontrol/erp/kernel/config/OpenApiConfig.kt
- [x] T042 [P] Add structured logging and Prometheus metrics for plugin status and event outbox lag in backend/src/main/kotlin/org/opencontrol/erp/kernel/metrics/ExtensionMetrics.kt
- [x] T043 Update module documentation and architectural README in backend/README.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories.
- **User Stories (Phases 3, 4, 5)**:
  - User Story 1 (P1 - Lifecycle) depends on Phase 2.
  - User Story 2 (P2 - SPIs) depends on Phase 2 and hooks into Plugin Lifecycle (T020).
  - User Story 3 (P3 - Event Bus) depends on Phase 2 and integrates with the domain event model.
- **Polish (Phase 6)**: Depends on completion of desired user story phases.

### User Story Dependencies

- **User Story 1 (P1)**: Independent of US2 and US3. Delivers the core MVP.
- **User Story 2 (P2)**: Extends US1 by registering SPI providers during activation, but can be verified with mock providers independently.
- **User Story 3 (P3)**: Operates independently using core event publisher, dispatching events to registered plugin listeners.

---

## Parallel Execution Examples

### User Story 1 Parallel Tasks
```bash
# Launch JPA entities and repositories in parallel:
Task: T013 "Create JPA entity ExtensionPlugin in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/domain/ExtensionPlugin.kt"
Task: T014 "Create JPA entity ExtensionDependency in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/domain/ExtensionDependency.kt"
Task: T015 "Create JPA entity ExtensionAuditLog in backend/src/main/kotlin/org/opencontrol/erp/kernel/plugin/domain/ExtensionAuditLog.kt"
```

### User Story 2 Parallel Tasks
```bash
# Launch SPI contracts and entities in parallel:
Task: T025 "Create core SPI marker interface ExtensionPoint in backend/src/main/kotlin/org/opencontrol/erp/kernel/spi/ExtensionPoint.kt"
Task: T026 "Create ExecutionContext data class in backend/src/main/kotlin/org/opencontrol/erp/kernel/spi/ExecutionContext.kt"
Task: T027 "Create JPA entity ExtensionPointRegistration in backend/src/main/kotlin/org/opencontrol/erp/kernel/spi/domain/ExtensionPointRegistration.kt"
```

### User Story 3 Parallel Tasks
```bash
# Launch Outbox entities and repositories in parallel:
Task: T034 "Create JPA entity EventOutbox in backend/src/main/kotlin/org/opencontrol/erp/kernel/event/domain/EventOutbox.kt"
Task: T035 "Create JPA entity EventDeliveryAttempt in backend/src/main/kotlin/org/opencontrol/erp/kernel/event/domain/EventDeliveryAttempt.kt"
Task: T036 "Create Spring Data JPA repository for EventOutbox in backend/src/main/kotlin/org/opencontrol/erp/kernel/event/repository/EventOutboxRepository.kt"
```

---

## Implementation Strategy

### MVP Scope (User Story 1 Only)
1. Complete **Phase 1: Setup** (T001 - T004).
2. Complete **Phase 2: Foundational** (T005 - T008).
3. Complete **Phase 3: User Story 1** (T009 - T021).
4. Run integration tests to prove that plugins can be uploaded, installed, activated, and deactivated dynamically.

### Incremental Delivery
- **Release 0.1 (MVP)**: Phase 1 + Phase 2 + Phase 3 (Plugin Lifecycle management in Kotlin).
- **Release 0.2**: Phase 4 (Service Provider Interface registry and contextual resolution).
- **Release 0.3**: Phase 5 (Unified Event Bus with Transactional Outbox).
- **Release 0.4**: Phase 6 (Polish, Metrics, OpenAPI, and End-to-End Validation).
