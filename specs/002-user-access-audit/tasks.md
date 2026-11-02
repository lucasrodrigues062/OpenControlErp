# Tasks: User Management, Access Control (RBAC), and Audit Trail

**Input**: Design documents from `/specs/002-user-access-audit/`  
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md), [research.md](research.md)  
**Tests**: Unit and integration test tasks are included per the project quality standards and constitution (Principle VII: >= 90% coverage gate).

## Format: `- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Explicit file paths included in all tasks

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database schema initialization and frontend project bootstrapping.

- [x] T001 Create Flyway database migration `V1_0_5__create_iam_and_audit_tables.sql` defining `iam_profile`, `iam_profile_permission`, `iam_user`, and `audit_log` tables in backend/src/main/resources/db/migration/V1_0_5__create_iam_and_audit_tables.sql
- [x] T002 [P] Initialize frontend application workspace with Vite, React 19, TypeScript, and package scripts in frontend/package.json and frontend/vite.config.ts
- [x] T003 [P] Configure Tailwind CSS, PostCSS, and base design tokens (colors, typography, surfaces) per `ui-ux-pro-max` in frontend/tailwind.config.js and frontend/src/index.css
- [x] T004 [P] Install and configure frontend dependencies (TanStack Query, React Router, Lucide React, class-variance-authority, clsx, tailwind-merge) in frontend/package.json

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core security, token handling, and foundational UI components that MUST be complete before user story flows can execute.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T005 [P] Create Spring Security configuration (`SecurityConfig`, password encoder, SecurityContext integration) in backend/src/main/kotlin/org/opencontrol/erp/iam/config/SecurityConfig.kt
- [x] T006 [P] Implement JWT service (token generation, parsing, validation, and tenant/profile claims extraction) in backend/src/main/kotlin/org/opencontrol/erp/iam/service/JwtService.kt
- [x] T007 [P] Create base API HTTP client and authentication state context in frontend/src/api/client.ts and frontend/src/context/AuthContext.tsx
- [x] T008 [P] Create reusable core UI design system components (Button, Input, Card, Modal/Dialog, Drawer, Badge, Table, Toast) in frontend/src/components/ui/ per `ui-styling` skill
- [x] T009 [P] Create application layout shell (Sidebar navigation, Header with user profile menu, breadcrumbs) in frontend/src/components/layout/AppLayout.tsx

**Checkpoint**: Foundation ready - user story implementation can now begin.

---

## Phase 3: User Story 1 - Profile & Permission Configuration (Priority: P1) 🎯 MVP

**Goal**: Enable administrators to define authorization profiles (roles) and assign granular permissions that dictate what actions are allowed or restricted.

**Independent Test**: Create a profile (e.g. "Sales Operator"), select allowed capabilities, verify it can be saved, listed, updated, and that duplicate names are rejected.

### Tests for User Story 1
- [x] T010 [P] [US1] Unit test for Profile domain validation and duplicate name rejection in backend/src/test/kotlin/org/opencontrol/erp/iam/service/ProfileServiceTest.kt
- [x] T011 [P] [US1] Integration test for Profile lifecycle, permission persistence, and deletion blocking when users are assigned in backend/src/test/kotlin/org/opencontrol/erp/iam/api/ProfileControllerTest.kt

### Implementation for User Story 1
- [x] T012 [P] [US1] Create JPA entity `Profile` with `name`, `description`, `is_system`, and unique constraint per tenant in backend/src/main/kotlin/org/opencontrol/erp/iam/domain/Profile.kt
- [x] T013 [P] [US1] Create system `Permission` enum/catalog and normalized entity `ProfilePermission` in backend/src/main/kotlin/org/opencontrol/erp/iam/domain/Permission.kt
- [x] T014 [P] [US1] Create Spring Data JPA repository `ProfileRepository` with custom tenant queries in backend/src/main/kotlin/org/opencontrol/erp/iam/repository/ProfileRepository.kt
- [x] T015 [US1] Implement `ProfileService` (create, update, delete preventing deletion if assigned to users, permission toggling) in backend/src/main/kotlin/org/opencontrol/erp/iam/service/ProfileService.kt
- [x] T016 [US1] Implement `ProfileController` and DTOs per OpenAPI contract (`/api/v1/iam/profiles`, `/api/v1/iam/permissions`) in backend/src/main/kotlin/org/opencontrol/erp/iam/api/ProfileController.kt

**Checkpoint**: User Story 1 is functional, delivering profile creation and permission catalog endpoints.

---

## Phase 4: User Story 2 - User Lifecycle and Single-Profile Association (Priority: P1)

**Goal**: Register and manage user accounts with secure authentication and strict single-profile assignment, evaluating permissions on protected actions.

**Independent Test**: Register a new user linked to a single profile, authenticate to obtain a JWT, verify allowed actions succeed, and verify actions requiring missing permissions return 403 Forbidden.

### Tests for User Story 2
- [x] T017 [P] [US2] Unit test for User domain validation, password hashing, and strict single profile assignment in backend/src/test/kotlin/org/opencontrol/erp/iam/service/UserServiceTest.kt
- [x] T018 [P] [US2] Integration test for user authentication, single-profile assignment, and RBAC authorization evaluation in backend/src/test/kotlin/org/opencontrol/erp/iam/api/UserControllerTest.kt

### Implementation for User Story 2
- [x] T019 [P] [US2] Create JPA entity `User` with `ManyToOne` profile reference (`NOT NULL`), status enum in backend/src/main/kotlin/org/opencontrol/erp/iam/domain/User.kt
- [x] T020 [P] [US2] Create Spring Data JPA repository `UserRepository` with tenant isolation in backend/src/main/kotlin/org/opencontrol/erp/iam/repository/UserRepository.kt
- [x] T021 [US2] Implement `UserService` (register, update, toggle active/inactive, last admin protection) in backend/src/main/kotlin/org/opencontrol/erp/iam/service/UserService.kt
- [x] T022 [US2] Implement `AuthenticationService` and `AuthController` (`/api/v1/auth/login`) in backend/src/main/kotlin/org/opencontrol/erp/iam/service/AuthenticationService.kt and backend/src/main/kotlin/org/opencontrol/erp/iam/api/AuthController.kt
- [x] T023 [US2] Implement `UserController` and DTOs (`/api/v1/iam/users`) in backend/src/main/kotlin/org/opencontrol/erp/iam/api/UserController.kt

**Checkpoint**: User Stories 1 and 2 work seamlessly together, providing full identity and access management.

---

## Phase 5: User Story 3 - Unified Audit Trail (Who, What, When) (Priority: P2)

**Goal**: Provide an immutable, transactional audit trail recording actor, action, target entity, state diffs, and timestamp for all entity mutations.

**Independent Test**: Mutate a User or Profile, query `/api/v1/audit/logs`, and verify an immutable entry exists with actor details and JSON before/after snapshots.

### Tests for User Story 3
- [x] T024 [P] [US3] Unit test for audit diff generation and actor extraction in backend/src/test/kotlin/org/opencontrol/erp/audit/service/AuditServiceTest.kt
- [x] T025 [P] [US3] Integration test verifying automatic audit trail creation on User and Profile mutations in backend/src/test/kotlin/org/opencontrol/erp/audit/AuditIntegrationTest.kt

### Implementation for User Story 3
- [x] T026 [P] [US3] Create JPA entity `AuditLog` with immutable JSONB diffs (`state_before`, `state_after`) in backend/src/main/kotlin/org/opencontrol/erp/audit/domain/AuditLog.kt
- [x] T027 [P] [US3] Create Spring Data JPA repository `AuditLogRepository` with indexed queries in backend/src/main/kotlin/org/opencontrol/erp/audit/repository/AuditLogRepository.kt
- [x] T028 [US3] Implement `AuditService` and JPA Entity Listener `AuditEntityListener` intercepting entity mutations in backend/src/main/kotlin/org/opencontrol/erp/audit/service/AuditService.kt and backend/src/main/kotlin/org/opencontrol/erp/audit/listener/AuditEntityListener.kt
- [x] T029 [US3] Implement `AuditController` and DTOs (`/api/v1/audit/logs`) per OpenAPI contract in backend/src/main/kotlin/org/opencontrol/erp/audit/api/AuditController.kt

**Checkpoint**: All domain entity mutations automatically create immutable audit log entries.

---

## Phase 6: User Story 4 - Modern Administrative Web Interface (Frontend) (Priority: P2)

**Goal**: Deliver a modern, accessible web interface for interactive authentication, user management, profile/permission configuration, and audit trail inspection.

**Independent Test**: Log in via the web application, create a user and profile via interactive dialogs, inspect the permission matrix, and view the updated audit explorer drawer.

### Tests for User Story 4
- [x] T030 [P] [US4] Frontend component tests for Login, Users, and Permission matrix in frontend/src/pages/__tests__/UsersPage.test.tsx

### Implementation for User Story 4
- [x] T031 [P] [US4] Implement API client integration modules (`authApi.ts`, `usersApi.ts`, `profilesApi.ts`, `auditApi.ts`) in frontend/src/api/
- [x] T032 [P] [US4] Implement Login page with validation and session storage in frontend/src/pages/LoginPage.tsx
- [x] T033 [US4] Implement Users management page (table with search, status filters, create/edit modal with single-profile select) in frontend/src/pages/UsersPage.tsx
- [x] T034 [US4] Implement Profiles & Permission Matrix page (profile listing, create/edit modal with categorized checkbox grid) in frontend/src/pages/ProfilesPage.tsx
- [x] T035 [US4] Implement Audit Trail Explorer page (paginated log table, date range filters, lateral drawer showing JSON diff) in frontend/src/pages/AuditPage.tsx
- [x] T036 [P] [US4] Implement client-side permission gate component (`PermissionGate`) to conditionally show/hide UI action buttons in frontend/src/components/common/PermissionGate.tsx

**Checkpoint**: Full administrative web UI is functional and integrated with all backend services.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: System bootstrap, end-to-end integration validation, test coverage enforcement, and documentation.

- [x] T037 [P] Seed default system Administrator profile and initial bootstrap user in Flyway migration or application runner in backend/src/main/kotlin/org/opencontrol/erp/iam/config/IamBootstrapRunner.kt
- [x] T038 [P] Execute end-to-end scenarios documented in `quickstart.md` using an automated integration test in backend/src/test/kotlin/org/opencontrol/erp/QuickstartIamEndToEndTest.kt
- [x] T039 [P] Verify JaCoCo >= 90% test coverage verification across all new packages in backend/build.gradle.kts
- [x] T040 Update backend and frontend architectural documentation and README files in backend/README.md and frontend/README.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories.
- **User Stories (Phases 3, 4, 5, 6)**:
  - User Story 1 (P1 - Profiles) depends on Phase 2.
  - User Story 2 (P1 - Users) depends on US1 (needs Profile entity/repository for 1:N association).
  - User Story 3 (P2 - Audit) depends on Phase 2 and hooks into User/Profile entities.
  - User Story 4 (P2 - Frontend) depends on Backend API contracts (Phases 3, 4, 5).
- **Polish (Phase 7)**: Depends on completion of all user stories.

### User Story Dependencies

- **User Story 1 (P1)**: Independent of other stories; defines the Profile/Permission baseline.
- **User Story 2 (P1)**: Consumes Profile entity to enforce the single-profile invariant on users.
- **User Story 3 (P2)**: Operates across entities; audits mutations made by US1 and US2.
- **User Story 4 (P2)**: Provides the visual interface for US1, US2, and US3.

---

## Parallel Execution Examples

### User Story 1 Parallel Tasks
```bash
# Launch domain entities and repositories in parallel:
Task: T012 "Create JPA entity Profile in backend/src/main/kotlin/org/opencontrol/erp/iam/domain/Profile.kt"
Task: T013 "Create system Permission enum/catalog in backend/src/main/kotlin/org/opencontrol/erp/iam/domain/Permission.kt"
Task: T014 "Create ProfileRepository in backend/src/main/kotlin/org/opencontrol/erp/iam/repository/ProfileRepository.kt"
```

### User Story 2 Parallel Tasks
```bash
# Launch User entity and repository in parallel:
Task: T019 "Create JPA entity User in backend/src/main/kotlin/org/opencontrol/erp/iam/domain/User.kt"
Task: T020 "Create UserRepository in backend/src/main/kotlin/org/opencontrol/erp/iam/repository/UserRepository.kt"
```

### User Story 4 Parallel Tasks
```bash
# Launch frontend client modules and pages in parallel:
Task: T031 "Implement API client integration modules in frontend/src/api/"
Task: T032 "Implement Login page in frontend/src/pages/LoginPage.tsx"
Task: T036 "Implement PermissionGate in frontend/src/components/common/PermissionGate.tsx"
```

---

## Implementation Strategy

### MVP Scope (User Story 1 & 2)
1. Complete **Phase 1: Setup** (T001 - T004).
2. Complete **Phase 2: Foundational** (T005 - T009).
3. Complete **Phase 3: User Story 1** (T010 - T016) — Profile and Permission Management.
4. Complete **Phase 4: User Story 2** (T017 - T023) — User Account Lifecycle and Single-Profile Association.
5. Run integration tests to prove that users can authenticate and their permissions are evaluated.

### Incremental Delivery
- **Release 0.1 (IAM Core MVP)**: Phases 1, 2, 3, and 4 (Profiles + Users + RBAC).
- **Release 0.2 (Audit Trail)**: Phase 5 (Automated transactional mutation auditing).
- **Release 0.3 (Web UI Experience)**: Phase 6 (React administrative portal).
- **Release 0.4 (Hardening & Quality Gate)**: Phase 7 (Bootstrap, JaCoCo >= 90% verification, documentation).
