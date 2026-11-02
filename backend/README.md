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

4. **Identity & Access Management (IAM - Single-Profile RBAC & Preferences)**:
   - User account lifecycle with cryptographic BCrypt password hashing.
   - Strict 1:N relationship from `Profile` to `User` (a user belongs to exactly one profile).
   - Granular system permission catalog (`USERS_READ`, `USERS_WRITE`, `PROFILES_MANAGE`, `AUDIT_READ`).
   - Cross-device user preferences (`preferred_language` supporting `en-US` and `pt-BR`) via `PATCH /api/v1/iam/users/me/preferences`.
   - Stateless JWT authentication filter with claim extraction into `SecurityContextHolder`.

5. **Unified Audit Trail**:
   - Immutable, append-only `audit_log` table storing actor identity, action type, entity type/ID, and timestamp.
   - Transaction-bound capture of before and after snapshots in PostgreSQL `JSONB`.

6. **Dynamic Custom Business Rules Engine**:
   - Multi-tenant rule configuration attached to lifecycle triggers (`sale.item_added`, `sale.finalized`, `financial.title_settled`, `product.created`, etc.).
   - Hybrid authoring: Visual No-Code Builder (entity attributes, comparison operators, and action templates) + Advanced SpEL DSL expressions.
   - Dual-timing hooks: Synchronous `BEFORE` transaction-blocking checks vs Asynchronous `AFTER` Virtual Thread events.
   - Execution safety: Sandboxed SpEL via `SimpleEvaluationContext`, recursion guards ($\le 5$), timeout circuit breakers, and configurable failure policies (`ABORT_TRANSACTION` vs `LOG_AND_CONTINUE`).
   - Dry-run simulation endpoint (`POST /api/v1/rules/test`) and persistent execution traces (`rule_execution_trace`).

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
