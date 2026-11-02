# Architectural Research & Technical Decisions: User Management, Access Control (RBAC), and Audit Trail

## 1. Authentication & Security Architecture

### Decision
Implement stateless token-based authentication (JWT - RFC 7519) combined with Spring Security and BCrypt password hashing (work factor 12). The access token encapsulates user identity (`userId`, `username`, `tenantId`, and `profile`).

### Rationale
- **Stateless SPA Architecture**: Aligns directly with modern React single-page application (SPA) patterns without requiring server-side HTTP session clustering.
- **Tenant Context Propagation**: The JWT claims cleanly hydrate the kernel's `SecurityContextHolder` (`tenantId`, `userId`, `userRoles`) on every authenticated HTTP request.
- **Performance**: High-throughput authorization checks without requiring database lookups for session verification on every request.

### Alternatives Considered
- *Stateful HTTP Sessions (Spring Session with Redis)*: Rejected as it introduces external Redis infrastructure dependencies before required by scale.
- *External Identity Provider (Keycloak / Auth0)*: Evaluated as a potential future plugin, but rejected for the core kernel MVP to keep OpenControl self-contained and easy to deploy on-premise or in private clouds.

---

## 2. Access Control Model: Single-Profile RBAC

### Decision
Implement strict Role-Based Access Control (RBAC) with a direct `ManyToOne` relationship from `User` to `Profile`. Each `Profile` contains a normalized collection of granular string permissions (e.g., `USERS:READ`, `USERS:WRITE`, `PROFILES:MANAGE`, `AUDIT:READ`).

### Rationale
- **User Requirement Compliance**: Directly implements the user's specification: *"um perfil pode ter muitos usuários, mas um usuário só pode ter um único perfil. e dentro dos perfis eu devo ter a capacidade de informar o que está permitido ou não."*
- **Predictable Authorization**: A single profile per user eliminates conflicting role hierarchies and ambiguous precedence rules, making security audits straightforward and transparent.
- **Extensible Catalog**: Future plugins can register additional permission strings during their lifecycle, which automatically appear in the UI's permission matrix.

### Alternatives Considered
- *Multi-Role RBAC (User to many Roles)*: Rejected because the business rule strictly mandates single-profile assignment per user.
- *Attribute-Based Access Control (ABAC)*: Rejected as unnecessarily complex for the core authorization MVP; RBAC with domain permissions fully satisfies current and near-term enterprise requirements.

---

## 3. Unified Audit Trail Architecture (Who, What, When)

### Decision
Implement an automated audit trail mechanism using a JPA Entity Lifecycle Listener (`AuditEntityListener`) coupled with an `AuditService` and an append-only `audit_log` table storing before-and-after snapshots in PostgreSQL `JSONB`.

### Rationale
- **Transactional Consistency (Constitution Principle VI)**: The audit log record is committed within the same database transaction as the entity mutation; if the mutation rolls back, the audit record rolls back, preventing phantom audit entries.
- **Full Traceability**:
  - **Who**: `actor_id`, `actor_username`, `tenant_id` captured from `SecurityContextHolder`.
  - **What**: `entity_type`, `entity_id`, `action` (`CREATE`, `UPDATE`, `DELETE`), and `diff_json` (state before vs. state after).
  - **When**: `created_at` (UTC timestamp generated at commit).
  - **Where**: Client IP address and User-Agent captured from HTTP request context.
- **Non-repudiation**: Database constraints and lack of update/delete endpoints guarantee the audit trail is strictly immutable.

### Alternatives Considered
- *Hibernate Envers*: Considered for automatic schema versioning, but rejected because Envers generates separate `_AUD` tables for every single entity, making unified cross-entity chronological search and single-table querying across the entire ERP significantly more complex.
- *PostgreSQL Trigger Functions*: Evaluated, but triggers lack immediate access to application-level context (e.g., authenticated web user identity, tenant session, client IP) without custom PostgreSQL session variables.

---

## 4. Frontend Application Architecture & UI/UX Stack

### Decision
Create a dedicated `frontend/` workspace using **Vite + React (TypeScript) + Tailwind CSS + Radix UI / shadcn/ui components + TanStack Query + React Router**. Integrate design guidelines and tokens from `ui-ux-pro-max`, `ui-styling`, and `design-system` skills.

### Rationale
- **Constitution Principle V Compliance**: Component-driven architecture using modern TypeScript, Tailwind CSS, and accessible headless primitives (Radix UI).
- **Enterprise Design Standards**:
  - High information density suitable for ERP screens (users list, profile permission matrix, audit logs).
  - Semantic design tokens (primary, neutral, destructive, warning, surface, border) ensuring visual harmony with dark and light mode readiness.
  - Consistent component contracts (DataTable, Dialog/Modal, Drawer, Badge, Toast, Form controls).
- **Fast Developer Experience**: Vite provides near-instantaneous HMR and efficient production builds.

### Alternatives Considered
- *Next.js (SSR/App Router)*: Evaluated, but an ERP administrative portal is behind an authentication wall where Server-Side Rendering (SSR) offers minimal SEO benefit compared to the simplicity and fast operational response of a clean SPA.
- *Plain CSS / Bootstrap*: Rejected in favor of modern utility-first Tailwind CSS and headless accessible Radix UI primitives.
