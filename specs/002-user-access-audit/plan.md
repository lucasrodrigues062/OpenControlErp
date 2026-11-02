# Implementation Plan: User Management, Access Control (RBAC), and Audit Trail

**Branch**: `002-user-access-audit` | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-user-access-audit/spec.md`

## Summary

Deliver a secure, robust user management and access control (RBAC) foundation paired with an immutable audit trail system and a modern administrative React frontend. The backend implements user accounts with strict single-profile assignment, granular permission evaluation, JWT authentication, and automatic transaction-bound mutation auditing. The frontend provides a responsive, accessible interface with dedicated screens for Login, Users, Profiles (permission matrix), and an Audit Trail explorer, utilizing the `ui-ux-pro-max`, `ui-styling`, and `design-system` skills.

## Technical Context

**Language/Version**: 
- Backend: Kotlin 2.2 targeting JVM 21 LTS
- Frontend: TypeScript 5.x targeting ES2022+ / modern browsers

**Primary Dependencies**:
- Backend: Spring Boot 4.1.2, Spring Security (JWT + BCrypt), Spring Data JPA, Flyway, Jackson, SpringDoc OpenAPI 3.0
- Frontend: React 18/19, Vite, Tailwind CSS, Radix UI primitives, Lucide React, TanStack Query, React Router

**Storage**: PostgreSQL 16+ with Flyway migrations (`V1_0_5__create_iam_and_audit_tables.sql`)

**Testing**:
- Backend: JUnit 5, MockK, Spring Security Test, Testcontainers, JaCoCo (>= 90% coverage enforcement)
- Frontend: Vitest, React Testing Library

**Target Platform**: Modern web browsers (Desktop & Tablet) and Linux/JVM cloud/on-premise runtime

**Project Type**: Multi-tier Web Application (Modular Monolith backend + React SPA frontend)

**Performance Goals**:
- Authorization checks completed in < 5ms per protected request
- Audit trail queries across 100,000+ entries returned in < 500ms
- Frontend first contentful paint (FCP) < 1.5s

**Constraints**:
- Single-profile assignment per user (1:N relationship from Profile to Users)
- 100% audit capture on entity mutations (Constitution Principle VI)
- Minimum 90% automated test coverage gate on all new code (Constitution Principle VII)

## Constitution Check

| Principle | Status | Evaluation & Compliance Notes |
|---|---|---|
| **I. Modular Monolith & Clean Architecture** | PASS | Dedicated `iam` and `audit` packages within the core kernel boundary, communicating via interfaces and events. |
| **II. Extensibility via Plugin & App Architecture** | PASS | Extension engine can register new permissions dynamically via SPI and publish custom audit events. |
| **III. Brazil-First & Multi-Jurisdiction Abstraction** | PASS | Multi-tenant schema (`tenant_id` on all entities) supporting multi-branch and localization. |
| **IV. Codebase Language Standards** | PASS | 100% English domain terminology (`User`, `Profile`, `Permission`, `AuditLog`). |
| **V. Modern Componentized UI Design System** | PASS | React + TypeScript SPA in `frontend/` using Tailwind CSS, Radix UI, and tokens from `ui-ux-pro-max`. |
| **VI. Financial Immutability & Auditability** | PASS | Dedicated append-only `audit_log` table with before/after state diffs capturing actor, action, and timestamp. |
| **VII. Minimum 90% Test Coverage Gate** | PASS | Comprehensive unit and integration test suite planned for all services, controllers, and components. |
| **VIII. Mandatory Feature Documentation** | PASS | Spec, plan, research, data model, contracts (OpenAPI), and quickstart completed. |

## Project Structure

### Documentation (this feature)

```text
specs/002-user-access-audit/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Architectural research & technical decisions
├── data-model.md        # Entity definitions & schema constraints
├── quickstart.md        # End-to-end validation scenarios
├── checklists/
│   └── requirements.md  # Quality checklist
└── contracts/
    ├── auth-and-access-api.yaml # OpenAPI 3.0 REST specification
    └── audit-trail-schema.json  # Audit event payload JSON schema
```

### Source Code

```text
backend/
├── src/main/kotlin/org/opencontrol/erp/
│   ├── OpenControlApplication.kt
│   ├── audit/
│   │   ├── domain/AuditLog.kt
│   │   ├── listener/AuditEntityListener.kt
│   │   ├── repository/AuditLogRepository.kt
│   │   ├── service/AuditService.kt
│   │   └── api/AuditController.kt
│   ├── iam/
│   │   ├── config/SecurityConfig.kt
│   │   ├── domain/User.kt
│   │   ├── domain/Profile.kt
│   │   ├── domain/Permission.kt
│   │   ├── repository/UserRepository.kt
│   │   ├── repository/ProfileRepository.kt
│   │   ├── service/AuthenticationService.kt
│   │   ├── service/UserService.kt
│   │   ├── service/ProfileService.kt
│   │   └── api/
│   │       ├── AuthController.kt
│   │       ├── UserController.kt
│   │       ├── ProfileController.kt
│   │       └── dto/
│   └── kernel/
└── src/main/resources/
    └── db/migration/
        └── V1_0_5__create_iam_and_audit_tables.sql

frontend/
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
├── index.html
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── api/
    │   ├── client.ts
    │   ├── authApi.ts
    │   ├── usersApi.ts
    │   ├── profilesApi.ts
    │   └── auditApi.ts
    ├── components/
    │   ├── layout/ (Sidebar, Header, AppLayout)
    │   ├── ui/ (Button, Input, Table, Modal, Drawer, Badge, Switch, Toast)
    │   └── common/ (ProtectedRoute, PermissionGate)
    ├── pages/
    │   ├── LoginPage.tsx
    │   ├── UsersPage.tsx
    │   ├── ProfilesPage.tsx
    │   └── AuditPage.tsx
    └── types/
        ├── auth.ts
        ├── user.ts
        ├── profile.ts
        └── audit.ts
```

## Complexity Tracking

*No constitutional violations identified. Standard Clean Architecture and Modular Monolith principles applied.*
