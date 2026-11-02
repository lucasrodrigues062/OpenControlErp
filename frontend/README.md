# OpenControl ERP - Frontend Portal

Enterprise single-page application (SPA) providing an accessible, componentized user interface for OpenControl ERP.

## Technology Stack

- **Framework**: [React 18/19](https://react.dev/) + [TypeScript](https://www.typescriptlang.org/)
- **Build Tool**: [Vite](https://vitejs.dev/)
- **Styling**: [Tailwind CSS](https://tailwindcss.com/) with enterprise design tokens
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
│   │   ├── common/   # PermissionGate, ProtectedRoute
│   │   ├── layout/   # AppLayout (Sidebar, Header)
│   │   └── ui/       # Button, Input, Card, Badge, Dialog, Drawer
│   ├── context/      # AuthContext (JWT session state, permission evaluation)
│   ├── lib/          # Utilities (cn / tailwind-merge)
│   ├── pages/        # LoginPage, UsersPage, ProfilesPage, AuditPage
│   └── types/        # TypeScript interfaces for IAM and Audit domains
├── index.html
├── tailwind.config.js
└── vite.config.ts
```

## Available Scripts

- `npm run dev`: Launch local development server on `http://localhost:5173` (with `/api` proxied to backend).
- `npm run build`: Type-check with `tsc` and produce optimized production bundle in `dist/`.
- `npm test`: Run frontend unit tests using Vitest.
