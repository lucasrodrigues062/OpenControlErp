# Feature Specification: Frontend Multi-Language Support & User Language Preference

**Feature Branch**: `003-user-language-preference`

**Created**: 2026-11-02

**Status**: Draft

**Input**: User description: "Toda feature, tem que ter no frontend a linguagem, e deve ter um parametro para o usuario escolher a linguagem que prefere, de inicio, vamos comecar com Ingles US e Portugues Br"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Dynamic Language Switching in the Interface (Priority: P1) 🎯 MVP

As any user (authenticated or unauthenticated on the login page), I want to switch the application interface language between English (en-US) and Brazilian Portuguese (pt-BR) at any time from an accessible selector in the interface, so that I can interact with OpenControl ERP in my preferred language.

**Why this priority**: Core baseline requirement. Enables immediate localization of the application and validates the user journey of switching languages on demand without requiring a full page reload.

**Independent Test**:
Open the application on the Login page or Dashboard, open the language selector, switch from English (en-US) to Portuguese (pt-BR), and verify all navigation titles, buttons, dialog titles, table headers, and form labels immediately translate into Brazilian Portuguese. Switch back to English and verify immediate translation back.

**Acceptance Scenarios**:
1. **Given** the user is viewing the application in English (`en-US`), **When** the user selects "Português (Brasil)" from the language switcher dropdown in the header or login screen, **Then** all visible text (navigation, actions, headings, statuses) switches to Brazilian Portuguese immediately without page reload.
2. **Given** the user changes language to `pt-BR`, **When** the user refreshes the page or reopens the browser, **Then** the application remembers and loads in `pt-BR`.
3. **Given** a new visitor opens the application for the first time without prior saved preference, **When** the browser locale is detected as `pt` or `pt-BR`, **Then** the application defaults to `pt-BR`; otherwise, it defaults to `en-US`.

---

### User Story 2 - User Account Language Preference Persistence (Priority: P2)

As an authenticated user, I want my language preference to be associated with my user profile, so that whenever I log in from any computer or browser, the system automatically respects my chosen language.

**Why this priority**: Enhances user experience across multiple devices and corporate workstations.

**Independent Test**:
Log in as a user, change the language preference to Portuguese (Brazil) in profile settings, log out, log in on an incognito window/different browser, and verify the user session automatically adopts Portuguese upon login.

**Acceptance Scenarios**:
1. **Given** an authenticated user updates their language preference in profile settings, **When** saved, **Then** the preference is persisted to the backend user profile and synchronized with the frontend session.
2. **Given** a user has set `pt-BR` in their profile, **When** they log in on a fresh browser, **Then** the interface automatically renders in `pt-BR` upon successful authentication.

---

### User Story 3 - Localization Architecture Standard for Future Modules (Priority: P2)

As a developer or system maintainer, I want a standardized i18n structure and catalog organization, so that every upcoming feature or module (Master Data, Fiscal, Inventory, Financial) can easily register its own localized strings for both `en-US` and `pt-BR` without breaking existing translations or duplicating keys.

**Why this priority**: Guarantees architectural compliance and sustainability for the entire OpenControl ERP roadmap ("toda feature tem que ter no frontend a linguagem").

**Independent Test**:
Register a new test translation namespace or key in both `en-US` and `pt-BR`, verify typed translation helpers resolve the key, and verify that if a key is missing in `pt-BR`, it falls back cleanly to `en-US` without runtime crash or broken markup.

**Acceptance Scenarios**:
1. **Given** a missing translation key in `pt-BR`, **When** rendered by the UI, **Then** the system gracefully falls back to the `en-US` equivalent without displaying raw placeholder brackets or causing a render error.
2. **Given** date, time, currency, or numerical amounts displayed on screen, **When** the active language is `pt-BR`, **Then** dates format as `DD/MM/YYYY` and currency formats as `R$ #.##0,00`; when `en-US`, dates format as `MM/DD/YYYY` and currency formats as `$ #,##0.00`.

---

### Edge Cases

- **Missing Translation Keys**: If a module introduces a string present only in `en-US`, the translation engine must fall back to `en-US` instead of displaying empty text or crashing.
- **Unauthenticated / Login Screen Translation**: Users must be able to switch languages before logging in (on the Login page) without requiring an active JWT or server call.
- **Concurrent Device Preference Changes**: If a user updates their preference on Device A, Device B updates its local preference on the next profile refresh or login.
- **Invalid Stored Locale**: If the browser's local storage contains an unrecognized or corrupted locale code, the system must fall back to the default `en-US`.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a visible and accessible language selector in the application layout (Header and Login screen).
- **FR-002**: System MUST support English (`en-US`) and Brazilian Portuguese (`pt-BR`) as supported system languages.
- **FR-003**: System MUST translate all interface elements including:
  - Navigation menus (Users, Profiles & Permissions, Audit Trail).
  - Common actions (Create, Edit, Delete, Save, Cancel, Search, Filter, Sign Out).
  - Status badges (Active, Inactive, System, Allowed, Restricted).
  - Dialog and drawer titles, form field labels, and validation placeholders.
  - Audit log action names, entity types, and timestamps.
- **FR-004**: System MUST store the user's active language preference in browser local storage for immediate persistence.
- **FR-005**: System MUST allow authenticated users to persist their preferred language in their user profile record in the database.
- **FR-006**: System MUST auto-detect browser language upon initial visit when no saved preference exists, defaulting to `pt-BR` if browser locale starts with `pt`, otherwise `en-US`.
- **FR-007**: System MUST provide localized formatting helpers for dates, times, and numbers based on the active locale.
- **FR-008**: System MUST implement a fallback mechanism where missing localized strings in secondary languages default to `en-US`.
- **FR-009**: All existing pages (Login, Users, Profiles, Audit) and future modules MUST organize translation strings into structured domain namespaces (`common`, `auth`, `iam`, `audit`).

---

### Key Entities *(include if feature involves data)*

- **Language / Locale Catalog**:
  - `code`: ISO locale identifier (`en-US`, `pt-BR`).
  - `label`: Native display name (e.g., "English (US)", "Português (Brasil)").
  - `flag` / `icon`: Visual indicator for quick recognition.
- **User Preference**:
  - `preferred_language`: ISO code stored in the user record (nullable, defaults to system default if not specified).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Language switching takes effect instantaneously across 100% of visible interface text in less than 50 milliseconds without reloading the browser.
- **SC-002**: 100% of all user-facing strings across Login, Users, Profiles, and Audit Trail pages are fully localized in both `en-US` and `pt-BR`.
- **SC-003**: 100% of new feature frontend templates adhere to the i18n architectural pattern by declaring translation dictionaries in both languages.
- **SC-004**: System successfully detects and sets language for first-time visitors based on browser locale with 0 user friction.

---

## Assumptions

- Initial rollout strictly targets `en-US` and `pt-BR`; additional languages (such as Spanish `es-ES`) can be added later by adding new dictionary files without code redesign.
- User profile entity in backend can be extended with an optional `preferred_language` column (e.g. `VARCHAR(10)` default `'en-US'`) or handled via frontend preferences with backend sync.
- Backend error messages returned in JSON responses continue to follow standard error codes, while user-friendly messages displayed on the frontend are translated via the frontend dictionary.
