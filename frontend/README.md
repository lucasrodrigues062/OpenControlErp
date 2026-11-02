# OpenControl ERP - Frontend Portal

Enterprise single-page application (SPA) providing an accessible, componentized, multi-language user interface for OpenControl ERP.

## Technology Stack

- **Framework**: [React 18/19](https://react.dev/) + [TypeScript](https://www.typescriptlang.org/)
- **Build Tool**: [Vite](https://vitejs.dev/)
- **Styling**: [Tailwind CSS](https://tailwindcss.com/) with enterprise design tokens
- **Internationalization (i18n)**: Fully-typed, domain-namespaced multi-language architecture (`en-US` and `pt-BR`)
- **Component Primitives**: [Radix UI](https://www.radix-ui.com/) (accessible headless primitives)
- **Data Fetching**: [TanStack Query](https://tanstack.com/query/latest) + [Axios](https://axios-http.com/)
- **Icons**: [Lucide React](https://lucide.dev/)
- **Routing**: [React Router](https://reactrouter.com/)

## Project Structure

```text
frontend/
├── src/
│   ├── api/          # Typed REST API clients (auth, users, profiles, audit)
│   ├── components/
│   │   ├── common/   # LanguageSwitcher, PermissionGate, ProtectedRoute
│   │   ├── layout/   # AppLayout (Sidebar, Header with LanguageSwitcher)
│   │   └── ui/       # Button, Input, Card, Badge, Dialog, Drawer
│   ├── context/      # AuthContext, I18nContext (t(), formatDate(), formatCurrency())
│   ├── locales/      # Structured translation dictionaries (en-US, pt-BR)
│   ├── lib/          # Utilities (cn / tailwind-merge)
│   ├── pages/        # LoginPage, UsersPage, ProfilesPage, AuditPage
│   └── types/        # TypeScript interfaces for IAM, Audit, and i18n
├── index.html
├── tailwind.config.js
└── vite.config.ts
```

## Internationalization (i18n) Standards

Every feature and view in OpenControl ERP MUST provide translations for all user-facing strings in **English (en-US)** and **Brazilian Portuguese (pt-BR)**.

### Adding Translations for a New Module

1. **Declare Namespace in `src/types/i18n.ts`**:
   Add the TypeScript interface for your module's keys to `TranslationDictionary`.
2. **Add English Reference Strings in `src/locales/en-US/index.ts`**:
   Provide the canonical English strings.
3. **Add Portuguese Translations in `src/locales/pt-BR/index.ts`**:
   Provide matching Brazilian Portuguese translations.
4. **Use `useI18n()` in Components**:
   ```tsx
   import { useI18n } from '../context/I18nContext';

   export const MyComponent: React.FC = () => {
     const { t, formatDate, formatCurrency } = useI18n();

     return (
       <div>
         <h1>{t('myModule.title')}</h1>
         <p>{t('myModule.greeting', { name: user.name })}</p>
         <span>{formatDate(record.createdAt)}</span>
         <span>{formatCurrency(record.totalAmount)}</span>
       </div>
     );
   };
   ```

### Locale Formatting Helpers

- `formatDate(date, options?)`: Automatically formats date according to active locale (`MM/DD/YYYY` for `en-US`, `DD/MM/YYYY` for `pt-BR`).
- `formatCurrency(amount)`: Formats monetary amounts in native format (`$ 1,234.56` for `en-US`, `R$ 1.234,56` for `pt-BR`).
- `formatNumber(amount)`: Formats numbers with correct decimal and thousand separators.

## Available Scripts

- `npm run dev`: Launch local development server on `http://localhost:5173` (with `/api` proxied to backend).
- `npm run build`: Type-check with `tsc` and produce optimized production bundle in `dist/`.
- `npm test`: Run frontend unit tests using Vitest.
