# Research: Frontend Multi-Language Support & User Language Preference

**Feature**: `003-user-language-preference`  
**Date**: 2026-11-02  
**Status**: Completed  

---

## Technical Decisions & Trade-offs

### 1. Internationalization (i18n) Architecture for Frontend

#### Decision: Typed Enterprise i18n Provider with Domain Namespaces
We implement an architecture with:
1. Strongly-typed dictionary definitions for each locale (`en-US` as the reference canonical schema, `pt-BR` adhering strictly to that schema).
2. Domain-divided translation namespaces:
   - `common`: Generic UI actions (Save, Cancel, Delete, Edit, Search, Filter, Loading, Page, Rows, Actions).
   - `auth`: Login form, username, password, invalid credentials, sign in, sign out.
   - `iam`: Users page, profiles page, permissions matrix, status badges, roles.
   - `audit`: Audit trail explorer, before/after diffs, actor, entity types, timestamp formatters.
3. React `I18nContext` providing:
   - `currentLanguage`: `'en-US' | 'pt-BR'`
   - `setLanguage(lang: SupportedLanguage)`
   - `t(key: string, params?: Record<string, string | number>): string`
   - `formatDate(date: Date | string, options?: Intl.DateTimeFormatOptions): string`
   - `formatCurrency(amount: number): string`

#### Rationale:
- **Zero build breaking**: Pure TypeScript ensures that if a key is modified in `en-US`, compilation errors immediately identify any unaligned `pt-BR` translation.
- **Synchronous switching**: Switching the language triggers a simple React context update that instantly re-renders all components with `<50ms` latency and 0 network requests.
- **Scalability**: New features simply add a new sub-object or namespace file to `locales/en-US/` and `locales/pt-BR/`.

#### Alternatives Considered:
- *External Heavy i18n libraries (`react-intl`, `react-i18next`)*: Evaluated. While mature, adding heavy external runtime bundler plugins can complicate TypeScript type inference on keys. A tailored, typed React context wrapping standardized JSON dictionaries gives compile-time safety and zero bundle overhead.

---

### 2. User Preference Persistence Flow

#### Decision: Dual-Tier Persistence (localStorage + Backend User Record)
1. **Tier 1 (Browser Storage)**:
   - Active language code is cached in `localStorage` under `opencontrol_language`.
   - On initial app load (unauthenticated or pre-login), the system checks:
     1. `localStorage.getItem('opencontrol_language')`
     2. `navigator.language` (if starts with `pt`, select `pt-BR`; otherwise `en-US`)
     3. Fallback default: `en-US`
2. **Tier 2 (Account Synchronization)**:
   - When an authenticated user updates their language, the frontend updates `localStorage` immediately and dispatches `PATCH /api/v1/iam/users/me/preferences` with `{ "preferredLanguage": "pt-BR" }`.
   - Upon logging in, `LoginResponse` returns `preferredLanguage`, which synchronizes the frontend session automatically.

#### Rationale:
- Guarantees seamless localization on the login page before any credentials are entered.
- Automatically synchronizes preferences across company computers and mobile devices for authenticated users.

---

### 3. Database Schema Migration for User Language Preference

#### Decision: Flyway Migration `V1_0_6__add_user_language_preference.sql`
- Add column `preferred_language VARCHAR(10) NOT NULL DEFAULT 'en-US'` to `iam_user`.
- Update [`User.kt`](file:///c:/Projetos/OpenControlErp/backend/src/main/kotlin/org/opencontrol/erp/iam/domain/User.kt) with `@Column(name = "preferred_language") val preferredLanguage: String = "en-US"`.
- Update `UserResponseDto` and `LoginResponseDto` to include `preferredLanguage`.

#### Rationale:
- Follows Flyway sequential versioning standard (`V1_0_6`).
- Backward compatible with existing users (defaults to `en-US`).
