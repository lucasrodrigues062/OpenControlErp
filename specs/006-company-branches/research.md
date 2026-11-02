# Research: Company & Branch Management (Cadastro de Empresas e Filiais)

**Feature**: `006-company-branches`
**Date**: 2026-11-02

## 1. Domain Modeling: Unified vs Normalized Establishment Hierarchy

### Decision
Model establishments using a unified `CompanyBranch` entity with a self-referential parent-child hierarchy (`HEADQUARTERS` vs `BRANCH`), complemented by dedicated 1:1 satellite entities for Fiscal Configuration (`BranchFiscalSettings`), Digital Certificate (`BranchCertificate`), and Accountant Details (`BranchAccountant`).

### Rationale
- Brazilian businesses often start as a single establishment (Headquarters / Matriz) and expand into multiple branches (Filiais). Both Matriz and Filial share ~90% of identity, tax registration, address, and operational attributes.
- Modeling a single unified table `company_branches` with an explicit `type` enum (`HEADQUARTERS`, `BRANCH`) and an optional `parent_id` (referencing the Headquarters within the same `tenant_id`) avoids schema duplication while naturally supporting:
  - Single-establishment companies (`HEADQUARTERS` without branches).
  - Multi-establishment corporate groups (`HEADQUARTERS` + multiple `BRANCH` entities).
  - Multi-company holding tenants (multiple distinct `HEADQUARTERS`, each with their respective `BRANCH` children).
- Satellite tables (`branch_fiscal_settings`, `branch_certificates`, `branch_accountants`) prevent table bloat (as observed in legacy ERPs like `dbCronos` with 100+ columns), while ensuring separation of concerns: fiscal parameters and cryptographic keys can be queried and secured with dedicated access controls.

### Alternatives Considered
- **Separate `companies` and `branches` tables**: Rejected because a branch requires almost the exact same statutory data (CNPJ, IE, IM, Address, CNAE, Fiscal Regime) as a company. Having two tables creates duplicate queries and complicates generic modules (like Inventory and Financials) that need an establishment foreign key.
- **Flat 100-column monolithic table**: Directly replicating the legacy `dbo.Filiais` schema into a single Postgres table violates Clean Architecture and Principle I of the Constitution. Splitting into cohesive aggregates improves maintainability and performance.

---

## 2. Brazilian Fiscal Attributes & Reform Readiness (EC 132 / Reforma Tributária)

### Decision
Support classic Brazilian statutory attributes (CRT, CNAE, IE, IM, ICMS-SN, PIS/COFINS, FCP) alongside proactive hooks for Brazilian Tax Reform (Reforma Tributária - CBS / IBS rates).

### Rationale
- The user provided a real-world ERP DDL (`dbCronos.dbo.Filiais`) containing statutory fields:
  - CRT (`RegimeTributario`): 1 - Simples Nacional, 2 - Simples Nacional excesso sublimite, 3 - Regime Normal (Lucro Presumido / Real).
  - PIS/COFINS (`RegimeTributarioPISCOFINS`): Cumulativo vs Não Cumulativo.
  - State & Municipal: `InscricaoEstadual` (with exemption flag), `InscricaoMunicipal`, `mainCnae`, `nfseCnae`.
  - Tax options: `isTaxSubstitute`, `isIpiTaxpayer`, `calculateFcp`, `deductIcmsFromPiscofinsBasis` ("Tese do Século" STF tema 69).
  - Tax Reform (`AliqCBS`): Dual VAT transition (CBS federal, IBS estadual/municipal) commencing 2026/2027.
- Incorporating these statutory settings ensures OpenControl ERP is immediately ready for subsequent fiscal modules (NF-e, NFS-e, MDF-e, SPED Fiscal) without requiring database migrations later.

---

## 3. CNPJ Validation & Masking Standards

### Decision
Enforce official Receita Federal Modulo 11 check-digit verification in the backend service and on the frontend form, persisting the unmasked 14-digit string (`00000000000000`) while formatting for display (`00.000.000/0000-00`).

### Rationale
- CNPJ format: 8 digits root company + 4 digits branch (`0001` for headquarters, `0002+` for branches) + 2 verification check digits.
- Unmasked storage ensures uniform indexing and prevents regex search failures, while masking ensures human readability conforming to Brazilian standards.
- Unique index on `(tenant_id, cnpj)` guarantees that duplicate companies/branches cannot be accidentally registered within the same tenant.

---

## 4. Digital Certificate (A1 PKCS#12) Management & Security

### Decision
Store encrypted A1 Digital Certificates (`.pfx` / `.p12`) using PostgreSQL `BYTEA` encrypted at rest, coupled with metadata tracking:
- Serial number, Issuer (`cert_issuer`), Subject (`cert_subject`), CNPJ, and Expiration Date (`valid_until`).
- Proactive expiration warning service and status check.
- Password encrypted using AES-GCM kernel encryption service.

### Rationale
- Brazilian electronic fiscal documents (NF-e, NFS-e, MDF-e, CT-e) require an ICP-Brasil digital certificate (A1 file-based is standard for modern cloud ERPs).
- Storing certificate metadata allows the UI to display certificate health (e.g., "Valid until 15/12/2026", "Expires in 12 days", "Expired") without needing to decrypt and parse the private key on every request.

---

## 5. Active Branch Context & User Branch Segregation

### Decision
Implement multi-branch tenant scoping where:
1. `iam_user_branches` table maps users to permitted branches within their tenant, designating one as `is_default`.
2. The user session carries the active `branchId` (stored in state/header).
3. The frontend top navigation provides an active branch dropdown selector.
4. If a user has `ADMIN` role or `COMPANIES_MANAGE` global permission, they can access all branches in their tenant. Operational users are restricted to their assigned branch set.

### Rationale
- Operators in physical branch "Loja Centro" must not accidentally invoice or dispatch stock from "Loja Shopping".
- Instant switching in the top bar allows accountants and directors to seamlessly switch context without logging out.

---

## 6. Integration with Feature 005 (Dynamic Custom Fields)

### Decision
Register `COMPANY` in `CustomFieldEntityRegistry` so users can add custom dynamic fields (e.g., SUFRAMA registration, regional manager, branch square footage, specialized municipal permits) without altering source code.

### Rationale
- Meets the core vision established in Feature 005: allowing business-specific attributes for core master entities.
