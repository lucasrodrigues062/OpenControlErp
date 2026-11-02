# Technical Research: Extension Engine & Plugin Architecture

**Feature**: `001-extension-engine`  
**Date**: 2026-11-01  
**Status**: Completed

## 1. Programming Language & Runtime Ecosystem

### Decision
Adopt **Kotlin 2.x** targeting **JVM 21 LTS** with Spring Boot 4.1.2 and Gradle Kotlin DSL. Maintain full bi-directional interoperability with standard Java libraries (specifically Brazilian DFe open-source ecosystems such as Samuel Oliveira's repositories).

### Rationale
- **Compile-Time Null-Safety**: Eliminates `NullPointerException` risks across mission-critical accounting, ledger balancing, and fiscal calculation workflows.
- **Sealed Interfaces & Pattern Matching**: Perfect fit for modeling discrete plugin lifecycle states (`Discovered`, `Installed`, `Active`, `Inactive`, `Error`) and fiscal document states with compiler-enforced exhaustive checking.
- **Concise Domain Models**: Data classes, value objects, and extension functions dramatically reduce boilerplate compared to traditional enterprise Java while retaining bytecode compatibility.
- **Unified Kotlin Ecosystem**: Both backend application code and Gradle build scripts (`build.gradle.kts`) utilize a single, modern language syntax.

---

## 2. Plugin Packaging & Dynamic Runtime Loading

### Decision
Adopt a modular plugin loader supporting both built-in Spring Modulith beans (internal extensions) and dynamic external plugin archives (JAR/ZIP) loaded via isolated child `ClassLoader`s with structured manifest metadata (`opencontrol-plugin.json`).

### Rationale
- **Isolation**: Each external plugin receives a dedicated classloader inheriting from the application classloader to prevent classpath collision and allow clean unloading when an extension is deactivated.
- **Dynamic Lifecycle**: Administrators can install, activate, and deactivate plugins at runtime through REST APIs or the Admin UI without restarting the JVM process.
- **Built-in First**: Built-in modules (e.g., Brazilian Fiscal Engine adapter) implement the exact same SPI interfaces as dynamic plugins, ensuring a unified extension model across internal and third-party contributions.

### Alternatives Considered
- **OSGi (Apache Felix / Eclipse Equinox)**: Rejected due to prohibitive steep learning curve, fragile package exporting/importing rules, and friction with standard Spring Boot auto-configuration.
- **Pure Spring Boot Classpath Scanning**: Rejected because it requires restarting the JVM to add or remove extensions, failing user story P1.
- **WebAssembly (Wasm / GraalVM sandbox)**: Rejected for MVP due to performance overhead and impedance mismatch with JVM libraries (such as Samuel Oliveira's Java DFe dependencies).

---

## 3. Service Provider Interface (SPI) Contract & Resolution

### Decision
Define explicit Kotlin/Java interfaces extending a marker interface `ExtensionPoint`. Implement a centralized `ExtensionRegistry` maintaining registered providers ordered by declared `priority` (integer score) and evaluated against a `ContextMatcher` (evaluating conditions such as active Country/Jurisdiction, Company/Tenant ID, or Feature Flags).

### Rationale
- **Strong Typing**: Compile-time safety for core business contracts (e.g., `TaxCalculationSPI`, `FiscalEngineSPI`, `PaymentGatewaySPI`).
- **Contextual Fallbacks**: When an extension point has multiple implementations (e.g., a standard Brazilian tax calculator vs. an enterprise municipal tax plugin), the engine resolves the highest priority candidate matching the context, and smoothly falls back to `DefaultCoreProvider` if no plugin matches.
- **Alignment with Constitution**: Satisfies Principle II (Extensibility) and Principle III (Brazil-First Localization with Multi-Jurisdiction Abstraction).

### Alternatives Considered
- **`java.util.ServiceLoader`**: Rejected because it cannot dynamically register/deregister providers at runtime and does not offer contextual selection or priority weighting.
- **Spring `@ConditionalOnProperty`**: Rejected because conditions are only evaluated once at application startup, preventing dynamic runtime switching per tenant or transaction.

---

## 4. Unified Domain Event Bus Architecture

### Decision
Implement a dual-phase event pipeline:
1. **Pre-Commit Synchronous Interceptors (`@BeforeCommitHook`)**: Executed synchronously within the initiating database transaction boundary. Any interceptor can reject the operation by throwing a domain validation exception.
2. **Post-Commit Asynchronous Subscribers (`@AfterCommitEvent`)**: Executed after transaction commit using Java 21 Virtual Threads, backed by a persistent Transactional Outbox table (`event_outbox`) in PostgreSQL. 

### Rationale
- **Transactional Safety**: Financial and inventory mutations (Constitution Principle VI) cannot be corrupted by asynchronous side-effects.
- **Reliable Delivery & Fault Isolation**: If an external webhook or CRM listener fails, the primary business transaction has already succeeded. The event bus logs the failure in `event_delivery_attempt`, applies exponential backoff retry, and moves persistent failures to a Dead Letter Queue (DLQ) table for administrative replay.
- **Zero Extra Infrastructure**: Uses PostgreSQL (already in the stack) for the outbox table, eliminating the need for Kafka/RabbitMQ in early stages while maintaining durability.

### Alternatives Considered
- **Direct Asynchronous Spring `@Async`**: Rejected because in-memory task queues lose undelivered events if the server crashes or restarts during execution.
- **Apache Kafka / RabbitMQ**: Rejected as premature infrastructure overhead for a Modular Monolith, violating Principle I and YAGNI.

---

## 5. Lifecycle Management & Dependency Resolution

### Decision
Model the extension lifecycle as a Finite State Machine with explicit states:
`DISCOVERED` ➔ `INSTALLED` ➔ `ACTIVE` ⟷ `INACTIVE`, and `ERROR`.
Dependency validation uses a directed acyclic graph (DAG) topological sort (Kahn's Algorithm) to resolve startup/activation order and prevent circular dependencies.

### Rationale
- Prevents loading broken or conflicting extensions.
- Ensures dependencies are activated before dependents, and dependents are deactivated before prerequisites.
- Captures all transitions in an immutable `extension_audit_log` table.
