# OpenControl ERP - Backend Kernel

The OpenControl ERP Backend is an enterprise-grade Modular Monolith built with **Kotlin 2.x**, **Spring Boot 4.1.2**, and **PostgreSQL**, compiled and orchestrated via **Gradle**.

## Key Architecture Capabilities

1. **Plugin & App Runtime Engine**:
   - Discrete lifecycle states: `DISCOVERED`, `INSTALLED`, `ACTIVE`, `INACTIVE`, `ERROR`.
   - Isolated `PluginClassLoader` per plugin archive to prevent classpath leaks.
   - Directed Acyclic Graph (DAG) dependency resolution with circular dependency prevention (Kahn's algorithm).
   - Immutable audit trail (`extension_audit_log`).

2. **Service Provider Interface (SPI) Registry**:
   - Strongly-typed extensible contracts (`ExtensionPoint`, `ContextMatcher`).
   - Contextual matching (e.g. `jurisdiction="BR"`) with weighted priority resolution.
   - Resilient fallback to core implementations when external plugins are inactive.

3. **Unified Domain Event Bus & Transactional Outbox**:
   - Synchronous pre-commit validation interceptors (`PreCommitInterceptor`) that can abort operations.
   - Asynchronous post-commit dispatch backed by PostgreSQL `event_outbox` table.
   - Automatic exponential backoff retries and Dead-Letter Queue (DLQ) isolation.

## Building and Running

```bash
# Build with Gradle
./gradlew build

# Run tests
./gradlew test

# Start application locally
./gradlew bootRun
```

## API Documentation

When the application is running, Swagger UI is available at:
`http://localhost:8080/swagger-ui.html`

## Detailed Architectural Guides

- [Extension Engine & Plugin Developer Manual](../docs/architecture/extension-engine.md)
- [Project Constitution](../.specify/memory/constitution.md)
- [Feature 001 Specification](../specs/001-extension-engine/spec.md)
- [End-to-End Validation Quickstart](../specs/001-extension-engine/quickstart.md)
