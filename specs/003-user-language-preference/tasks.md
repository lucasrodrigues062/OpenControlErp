# Tasks: Frontend Multi-Language Support & User Language Preference

**Input**: Design documents from `/specs/003-user-language-preference/`  
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md), [research.md](research.md)  
**Tests**: Unit and integration tests are included per project quality standards and constitution (Principle VII: >= 90% coverage gate).

## Format: `- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Explicit file paths included in all tasks

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database schema migration and frontend type definitions.

- [x] T001 Create Flyway database migration `V1_0_6__add_user_language_preference.sql` adding `preferred_language VARCHAR(10) NOT NULL DEFAULT 'en-US'` to `iam_user` in backend/src/main/resources/db/migration/V1_0_6__add_user_language_preference.sql
- [x] T002 [P] Create TypeScript interfaces for supported languages and translation dictionary namespaces in frontend/src/types/i18n.ts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core dictionary catalogs and React i18n context that MUST be complete before user story UI flows execute.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T003 [P] Create canonical English (`en-US`) dictionary covering `common`, `auth`, `iam`, and `audit` namespaces in frontend/src/locales/en-US/index.ts
- [x] T004 [P] Create Brazilian Portuguese (`pt-BR`) dictionary covering `common`, `auth`, `iam`, and `audit` namespaces in frontend/src/locales/pt-BR/index.ts
- [x] T005 Implement `I18nContext` provider with `t()`, `currentLanguage`, `setLanguage`, `formatDate()`, `formatCurrency()`, `localStorage` persistence, and browser locale auto-detection in frontend/src/context/I18nContext.tsx
- [x] T006 [P] Create reusable `LanguageSwitcher` dropdown component with flags and labels in frontend/src/components/common/LanguageSwitcher.tsx
- [x] T007 Wrap application root with `I18nProvider` in frontend/src/App.tsx

**Checkpoint**: Foundation ready - user story implementation can now begin.

---

## Phase 3: User Story 1 - Dynamic Language Switching in the Interface (Priority: P1) 🎯 MVP

**Goal**: Enable any user to instantly switch the interface between English (`en-US`) and Brazilian Portuguese (`pt-BR`) with immediate reactivity and zero page reload.

**Independent Test**: Load the Login screen or Dashboard, click the language switcher, choose Brazilian Portuguese, and verify all text immediately renders in Portuguese without reloading the page. Switch back to English and verify immediate restoration.

### Tests for User Story 1
- [x] T008 [P] [US1] Unit tests for `I18nContext`, translation key resolution, missing key fallback to `en-US`, and date/currency formatters in frontend/src/pages/__tests__/I18n.test.tsx

### Implementation for User Story 1
- [x] T009 [P] [US1] Integrate `LanguageSwitcher` in login screen and localize all titles, input labels, buttons, and error messages in frontend/src/pages/LoginPage.tsx
- [x] T010 [P] [US1] Integrate `LanguageSwitcher` into top application header and localize navigation items and breadcrumbs in frontend/src/components/layout/AppLayout.tsx
- [x] T011 [US1] Localize Users management view (table headers, filters, dialog fields, status badges) in frontend/src/pages/UsersPage.tsx
- [x] T012 [US1] Localize Profiles and Permission matrix view (table headers, permission category cards, checkboxes, dialogs) in frontend/src/pages/ProfilesPage.tsx
- [x] T013 [US1] Localize Audit Trail Explorer view (table columns, date filters, lateral diff drawer) in frontend/src/pages/AuditPage.tsx

**Checkpoint**: User Story 1 is fully functional as an MVP — the complete admin interface is localized and switchable between English and Portuguese.

---

## Phase 4: User Story 2 - User Account Language Preference Persistence (Priority: P2)

**Goal**: Persist user language selection to the backend user account profile so preferences follow the user across browsers and devices.

**Independent Test**: Update language preference via `PATCH /api/v1/iam/users/me/preferences`, log in from a fresh browser session, and verify the user session automatically adopts the user's stored language upon authentication.

### Tests for User Story 2
- [x] T014 [P] [US2] Unit tests for user preference update and language validation in backend/src/test/kotlin/org/opencontrol/erp/iam/service/UserServiceTest.kt
- [x] T015 [P] [US2] Controller tests for `PATCH /api/v1/iam/users/me/preferences` in backend/src/test/kotlin/org/opencontrol/erp/iam/api/UserControllerTest.kt

### Implementation for User Story 2
- [x] T016 [P] [US2] Update `User` JPA entity with `@Column(name = "preferred_language")` mapping in backend/src/main/kotlin/org/opencontrol/erp/iam/domain/User.kt
- [x] T017 [P] [US2] Add `UserPreferencesDto` and update `UserResponseDto` / `UserSummaryDto` with `preferredLanguage` field in backend/src/main/kotlin/org/opencontrol/erp/iam/api/dto/UserDtos.kt
- [x] T018 [US2] Implement `updateUserPreferences` in `UserService` validating language code and recording audit trail in backend/src/main/kotlin/org/opencontrol/erp/iam/service/UserService.kt
- [x] T019 [US2] Expose `PATCH /api/v1/iam/users/me/preferences` endpoint in backend/src/main/kotlin/org/opencontrol/erp/iam/api/UserController.kt
- [x] T020 [P] [US2] Add `updatePreferences` API client function in frontend/src/api/usersApi.ts
- [x] T021 [US2] Synchronize user preferred language with `I18nContext` on login and preference update in frontend/src/context/AuthContext.tsx

**Checkpoint**: User Stories 1 and 2 operate seamlessly together, supporting both local browser selection and persistent cross-device account preferences.

---

## Phase 5: User Story 3 - Localization Architecture Standard for Future Modules (Priority: P2)

**Goal**: Establish a scalable dictionary registration standard so all future modules (Cadastros, Fiscal, Financeiro) easily include English and Portuguese translations.

**Independent Test**: Verify that a test translation key in `en-US` can be accessed via `t()`, and that if omitted from `pt-BR`, the application cleanly displays the `en-US` fallback without runtime error.

### Implementation for User Story 3
- [x] T022 [P] [US3] Document i18n architectural pattern and namespace extension guide in frontend/README.md

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: System bootstrap, end-to-end integration validation, test coverage enforcement, and documentation.

- [x] T023 [P] Execute end-to-end scenarios documented in `quickstart.md` using an automated integration test in backend/src/test/kotlin/org/opencontrol/erp/QuickstartIamEndToEndTest.kt
- [x] T024 [P] Run frontend test suite (`npm test`) and production bundle build (`npm run build`) in frontend/
- [x] T025 [P] Verify JaCoCo >= 90% test coverage verification across all new/modified packages in backend/build.gradle.kts
- [x] T026 Update backend and frontend architectural documentation in backend/README.md and frontend/README.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories.
- **User Stories (Phases 3, 4, 5)**:
  - User Story 1 (P1 - Dynamic Language Switching) depends on Phase 2.
  - User Story 2 (P2 - Backend Account Preference) depends on Phase 1 (migration) and Phase 2.
  - User Story 3 (P2 - Developer Standard) depends on Phase 2.
- **Polish (Phase 6)**: Depends on completion of all user stories.

---

## Parallel Execution Examples

### User Story 1 Parallel Tasks
```bash
# Localize individual views in parallel:
Task: T009 "Integrate LanguageSwitcher and localize LoginPage.tsx"
Task: T010 "Integrate LanguageSwitcher and localize AppLayout.tsx"
Task: T011 "Localize UsersPage.tsx"
```

### User Story 2 Parallel Tasks
```bash
# Update backend entity, DTOs, and frontend API in parallel:
Task: T016 "Update User JPA entity with preferred_language mapping"
Task: T017 "Add UserPreferencesDto and update UserResponseDto"
Task: T020 "Add updatePreferences API client in frontend/src/api/usersApi.ts"
```

---

## Implementation Strategy

### MVP Scope (User Story 1)
1. Complete **Phase 1: Setup** (T001 - T002).
2. Complete **Phase 2: Foundational** (T003 - T007) — Dictionaries, `I18nContext`, `LanguageSwitcher`.
3. Complete **Phase 3: User Story 1** (T008 - T013) — Full dynamic UI switching between English and Portuguese across all pages.
4. Verify MVP independently in the browser.

### Incremental Delivery
- **Release 0.1 (Frontend Dynamic i18n MVP)**: Phases 1, 2, and 3.
- **Release 0.2 (Backend Cross-Device Persistence)**: Phase 4.
- **Release 0.3 (Standardization & Quality Gate)**: Phases 5 and 6.
