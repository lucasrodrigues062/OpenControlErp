# Implementation Plan: Company & Branch Management (Cadastro de Empresas e Filiais)

**Branch**: `main` | **Date**: 2026-11-02 | **Spec**: [specs/006-company-branches/spec.md](spec.md)

**Input**: Feature specification from `specs/006-company-branches/spec.md` with Brazilian ERP DDL reference from `dbCronos.dbo.Filiais`.

## Summary

Implement multi-company and multi-branch management for OpenControl ERP, providing:
1. Unified establishment management (`company_branches`) supporting Headquarters (`HEADQUARTERS` / Matriz) and Branches (`BRANCH` / Filial) with parent-child hierarchy.
2. Complete Brazilian statutory and fiscal profile (`branch_fiscal_settings`) incorporating real-world attributes (CRT, PIS/COFINS regime, CNAE, IE, IM, IBGE city codes, tax exemptions, and Reforma Tributária CBS rates).
3. Secure A1 Digital Certificate (.pfx/.p12) upload and expiration tracking (`branch_certificates`).
4. Accountant records (`branch_accountants`).
5. User branch assignment and active branch switcher in the top navigation header (`iam_user_branch`).
6. Integration with dynamic custom fields (Feature 005) under entity `COMPANY`.

---

## Technical Context

**Language/Version**: Kotlin 2.x (JVM 21 LTS), TypeScript 5.x
**Primary Dependencies**: Spring Boot 4.1.2, Spring Data JPA, Hibernate, PostgreSQL, Flyway, Lucide React, Tailwind CSS, TanStack Query
**Storage**: PostgreSQL 16+ via Flyway migration `V1_0_9__create_company_branches_tables.sql`
**Testing**: JUnit 5, MockK, Spring Boot Test, JaCoCo ($\ge 90\%$ line and branch coverage), Vitest for frontend
**Target Platform**: Linux/Windows Web Server & Modern Browsers (Chrome, Firefox, Safari, Edge)
**Project Type**: Modular Monolith Web Application (`backend/` + `frontend/`)
**Performance Goals**: <50ms query time for active branch listing, instant (<200ms) branch context switching
**Constraints**: Zero plain-text private keys stored, strict multi-tenant isolation by `tenant_id`, full compliance with Brazilian CNPJ Modulo 11 validation
**Scale/Scope**: Supports multi-tenant corporate groups with dozens of branches per tenant

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Compliance Status | Justification / Implementation Strategy |
|-----------|-------------------|------------------------------------------|
| **I. Modular Monolith & Clean Architecture** | ✅ PASS | Implemented as a dedicated module `org.opencontrol.erp.companies` with distinct domain, repository, service, and API layers. |
| **II. Extensibility via Plugin & App Architecture** | ✅ PASS | Registers `COMPANY` in `CustomFieldEntityRegistry` allowing dynamic runtime extension without kernel schema modifications. |
| **III. Brazil-First Localization with Multi-Jurisdiction Abstraction** | ✅ PASS | Statutory Brazilian attributes (CNPJ, IE, IM, CRT, CNAE, IBGE codes, A1 Certificate) modelled cleanly with international address abstractions. |
| **IV. Codebase Language & Jurisdiction Nomenclature Standards** | ✅ PASS | English domain names (`CompanyBranch`, `BranchFiscalSettings`), preserving official Brazilian legal identifiers (`cnpj`, `stateRegistration`, `cnae`, `codigoIbge`). |
| **V. Modern Componentized UI with Design System Discipline** | ✅ PASS | Built with React, Tailwind CSS, accessible modals, tabs for Fiscal/Accountant/Certificate setup, and top header branch switcher. |
| **VI. Transactional Integrity, Financial Immutability & Auditability** | ✅ PASS | All mutations generate immutable audit logs (`audit_log`) capturing actor, action, and state diff. Hard deletion prohibited. |
| **VII. Minimum 90% Automated Test Coverage Gate** | ✅ PASS | Unit and integration tests cover CNPJ validator, services, controllers, and repository queries, with JaCoCo verification enforcing $\ge 90\%$. |
| **VIII. Mandatory Feature Documentation & Contract Transparency** | ✅ PASS | All Spec Kit artifacts (`spec.md`, `research.md`, `data-model.md`, `contracts/company-branches-api.yaml`, `quickstart.md`, `plan.md`) generated. |

---

## Project Structure

### Documentation (this feature)

```text
specs/006-company-branches/
├── spec.md              # Feature specification & requirements
├── plan.md              # This file (implementation plan)
├── research.md          # Domain decisions & fiscal analysis
├── data-model.md        # Relational schemas & ER diagram
├── quickstart.md        # Verification scenarios
├── contracts/
│   └── company-branches-api.yaml # OpenAPI 3.0 contract
└── checklists/
    └── requirements.md  # Quality checklist
```

### Source Code Layout

```text
backend/src/
├── main/
│   ├── kotlin/org/opencontrol/erp/companies/
│   │   ├── api/
│   │   │   ├── CompanyBranchController.kt
│   │   │   └── dto/CompanyBranchDtos.kt
│   │   ├── domain/
│   │   │   ├── CompanyBranch.kt
│   │   │   ├── BranchFiscalSettings.kt
│   │   │   ├── BranchCertificate.kt
│   │   │   ├── BranchAccountant.kt
│   │   │   ├── UserBranchAccess.kt
│   │   │   └── CompanyEnums.kt
│   │   ├── repository/
│   │   │   ├── CompanyBranchRepository.kt
│   │   │   ├── BranchFiscalSettingsRepository.kt
│   │   │   ├── BranchCertificateRepository.kt
│   │   │   ├── BranchAccountantRepository.kt
│   │   │   └── UserBranchAccessRepository.kt
│   │   ├── service/
│   │   │   ├── CompanyBranchService.kt
│   │   │   ├── CnpjValidator.kt
│   │   │   ├── BranchCertificateService.kt
│   │   │   └── BranchContextService.kt
│   │   └── exception/CompanyExceptions.kt
│   └── resources/db/migration/
│       └── V1_0_9__create_company_branches_tables.sql
└── test/
    └── kotlin/org/opencontrol/erp/companies/
        ├── service/CompanyBranchServiceTest.kt
        ├── service/CnpjValidatorTest.kt
        ├── api/CompanyBranchControllerTest.kt
        └── QuickstartCompanyBranchEndToEndTest.kt

frontend/src/
├── api/companyBranchesApi.ts
├── types/companyBranches.ts
├── context/BranchContext.tsx
├── components/companies/
│   ├── CompanyBranchFormModal.tsx
│   ├── CompanyBranchCard.tsx
│   ├── FiscalSettingsTab.tsx
│   ├── CertificateTab.tsx
│   ├── AccountantTab.tsx
│   └── BranchSwitcher.tsx
├── pages/CompaniesPage.tsx
└── locales/
    ├── en-US/companies.ts
    └── pt-BR/companies.ts
```

---

## Complexity Tracking

No constitution violations detected. Standard normalized design with clean value objects avoids monolith flat tables while fully fulfilling Brazilian statutory requirements.
