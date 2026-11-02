# Implementation Plan: Frontend Multi-Language Support & User Language Preference

**Branch**: `003-user-language-preference` | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-user-language-preference/spec.md`

## Summary

Implement full internationalization (i18n) for the OpenControl ERP platform supporting **English (US) (`en-US`)** and **Brazilian Portuguese (`pt-BR`)**. The architecture provides a dual-tier persistence system:
1. **Frontend (Browser-level)**: Dynamic language switcher dropdown in the application Header and Login screen, backed by typed JSON dictionaries organized into domain namespaces (`common`, `auth`, `iam`, `audit`), `localStorage` caching, browser language auto-detection, and localized formatting helpers (`Intl.DateTimeFormat`, `Intl.NumberFormat`).
2. **Backend (Account-level)**: PostgreSQL Flyway migration `V1_0_6` adding `preferred_language` to `iam_user`, DTO and JPA updates, and an endpoint (`PATCH /api/v1/iam/users/me/preferences`) allowing authenticated users to persist their language preference across devices.

## Technical Context

**Language/Version**: Kotlin 2.2 / JVM 21 LTS (Backend), TypeScript 5.6+ / React 18/19 (Frontend)  
**Primary Dependencies**: Spring Boot 4.1.2, Spring Security, Flyway, React, Lucide React, Tailwind CSS, TanStack Query  
**Storage**: PostgreSQL (`iam_user.preferred_language`) + Browser `localStorage`  
**Testing**: JUnit 5, MockK, SpringMockK, Vitest  
**Target Platform**: Modern Web Browsers (Chrome, Firefox, Safari, Edge) & JVM 21 on Linux/Windows  
**Project Type**: Full-stack Web Application (Modular Monolith Backend + Vite React SPA)  
**Performance Goals**: Language switch execution in `<50ms` with zero full-page reload and zero network call for client translations  
**Constraints**: Zero runtime errors on missing translation keys (graceful fallback to `en-US`); strictly maintain >= 90% test coverage gate on backend  
**Scale/Scope**: All existing pages (Login, Users, Profiles, Audit) + foundation for all future modules  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: Domain-Driven Modular Monolith**: User preference endpoint sits inside `iam` module; no cross-boundary leaks. (PASS)
- **Principle IV: Multi-Tenancy**: Language preference is user-level within tenant boundaries. (PASS)
- **Principle VII: Zero-Tolerance Documentation & Test Coverage**: Plan includes comprehensive unit and integration tests with JaCoCo verification gate >= 90%. (PASS)
- **Code Documentation & Language**: English remains the primary code and technical doc language, with user-facing translations cleanly isolated in dictionary assets. (PASS)

## Project Structure

### Documentation (this feature)

```text
specs/003-user-language-preference/
├── spec.md              # Feature specification
├── plan.md              # This implementation plan
├── research.md          # Technical research & decisions
├── data-model.md        # Database & DTO models
├── quickstart.md        # Runnable validation scenarios
├── contracts/
│   ├── user-preferences-api.yaml
│   └── i18n-dictionary-schema.json
└── checklists/
    └── requirements.md
```

### Source Code

```text
backend/
├── src/main/resources/db/migration/
│   └── V1_0_6__add_user_language_preference.sql
├── src/main/kotlin/org/opencontrol/erp/iam/
│   ├── api/
│   │   ├── UserController.kt             # Add preferences endpoint
│   │   └── dto/UserDtos.kt               # Add UserPreferencesDto
│   ├── domain/User.kt                    # Add preferredLanguage column
│   └── service/UserService.kt            # Add updatePreferences method
└── src/test/kotlin/org/opencontrol/erp/iam/
    ├── api/UserControllerTest.kt
    └── service/UserServiceTest.kt

frontend/
├── src/
│   ├── api/usersApi.ts                   # Add updatePreferences API call
│   ├── components/
│   │   ├── common/LanguageSwitcher.tsx   # Reusable selector with flags
│   │   └── layout/AppLayout.tsx          # Embed switcher in top header
│   ├── context/
│   │   ├── AuthContext.tsx               # Sync user preference on login
│   │   └── I18nContext.tsx               # Core i18n state, t() function, formatting
│   ├── locales/
│   │   ├── en-US/index.ts                # English reference dictionary
│   │   └── pt-BR/index.ts                # Brazilian Portuguese dictionary
│   ├── pages/
│   │   ├── LoginPage.tsx                 # Embed switcher & localize text
│   │   ├── UsersPage.tsx                 # Localize table & modal text
│   │   ├── ProfilesPage.tsx              # Localize table & modal text
│   │   └── AuditPage.tsx                 # Localize audit explorer text
│   └── types/
│       └── i18n.ts                       # Typed dictionary interfaces
└── src/pages/__tests__/
    └── I18n.test.tsx                     # Unit tests for translation & formatters
```

## Structure Decision

The implementation extends the existing `iam` backend module with a backward-compatible Flyway migration `V1_0_6` and builds a robust, modular translation layer in `frontend/src/locales/` that provides full typing and instant runtime reactivity.
