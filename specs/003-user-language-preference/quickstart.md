# Quickstart: Frontend Multi-Language Support & User Language Preference

**Feature**: `003-user-language-preference`  
**Purpose**: Runnable validation scenarios proving language switching and preference persistence work end-to-end.

---

## Scenario 1: Unauthenticated Language Switch on Login Screen

1. Open `http://localhost:5173/login` in a browser.
2. Verify the initial language displays in English (`en-US`):
   - Title: "Welcome Back"
   - Subtitle: "Sign in to access OpenControl ERP"
   - Button: "Sign In"
3. Click the language switcher dropdown in the upper right and select `Português (Brasil)`:
   - Title immediately becomes: "Bem-vindo de volta"
   - Subtitle becomes: "Acesse o OpenControl ERP"
   - Button becomes: "Entrar"
4. Refresh the page:
   - Page loads directly in Portuguese from `localStorage`.

---

## Scenario 2: Synchronous Interface Translation Across Modules

1. Log in with `admin` / `admin123`.
2. Navigate between `Usuários` (`/users`), `Perfis & Permissões` (`/profiles`), and `Trilha de Auditoria` (`/audit`):
   - Table columns show in Portuguese: "Nome Completo", "Email", "Perfil", "Status", "Ações".
   - Status badges display "Ativo", "Inativo", "Sistema".
3. Click the language selector in the top application header and choose `English (US)`:
   - All navigation items, table column headers, filter inputs, and badges switch instantly to English.
   - Header title displays "Users", "Profiles & Permissions", or "Audit Trail".

---

## Scenario 3: User Profile Language Preference Persistence

1. Send `PATCH /api/v1/iam/users/me/preferences` with payload:
   ```json
   {
     "preferredLanguage": "pt-BR"
   }
   ```
2. Verify response status is `200 OK` with:
   ```json
   {
     "preferredLanguage": "pt-BR"
   }
   ```
3. Log out and clear browser `localStorage`.
4. Log in again as `admin`:
   - System receives `preferredLanguage: "pt-BR"` from the login payload and immediately initializes the UI in Brazilian Portuguese.
