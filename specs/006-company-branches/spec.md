# Feature Specification: Company & Branch Management (Cadastro de Empresas e Filiais)

**Feature Branch**: `006-company-branches`

**Created**: 2026-11-02

**Status**: Draft

**Input**: User description: "Agora vamos para o cadastro de empresas/filiais"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register and Manage Headquarters Company (Empresa Matriz) (Priority: P1) 🎯 MVP

Tenant administrators need to register their primary legal entity (Empresa Matriz / Headquarters) with legal identification (Razão Social, Nome Fantasia), valid Brazilian tax identifiers (CNPJ, Inscrição Estadual, Inscrição Municipal), complete statutory address (with postal code CEP, street, number, neighborhood, city with IBGE code, and state UF), contact info, and tax regime (Simples Nacional, Lucro Presumido, Lucro Real).

**Why this priority**: Establishing the core company record is the foundational prerequisite for all operational modules in the ERP (fiscal document issuing, customer/supplier relationships, chart of accounts, inventory, and sales).

**Independent Test**: An administrator can access the Companies & Branches management interface, create a new Headquarters company with statutory Brazilian data and address, review it in the companies list, and edit its details.

**Acceptance Scenarios**:

1. **Given** an authenticated administrator with `COMPANIES_MANAGE` permission, **When** they fill in the legal name, trade name, valid CNPJ, tax regime, and complete address for a new Headquarters company, **Then** the company is persisted with active status and marked as Headquarters (`HEADQUARTERS`).
2. **Given** an administrator creating or editing a company, **When** they input an invalid CNPJ (incorrect format or invalid check digits), **Then** the system rejects the operation with a clear validation error indicating invalid CNPJ.
3. **Given** an existing company, **When** another user attempts to register a company with the same CNPJ within the same tenant, **Then** the system prevents creation with a duplicate CNPJ warning.

---

### User Story 2 - Register and Manage Operational Branches (Filiais) (Priority: P2)

Organizations with multiple physical stores, warehouses, or regional distribution units need to register branches (Filiais) linked to a parent Headquarters company, each with its own CNPJ (operational branch digits `/0002-XX`, `/0003-XX`), specific state and municipal tax registrations, local address, and branch code.

**Why this priority**: Multi-unit companies cannot operate retail stores, regional distribution hubs, or independent state tax registrations without branch isolation.

**Independent Test**: An administrator can register a branch linked to an existing Headquarters company, list all branches under the parent company, update branch data, and toggle branch operational status (active/inactive).

**Acceptance Scenarios**:

1. **Given** an existing Headquarters company, **When** an administrator adds a new operational branch with code `FILIAL-01`, branch-specific CNPJ, local address, and assigns it to the Headquarters, **Then** the branch is saved with a parent link and status `ACTIVE`.
2. **Given** an operational branch with active transactional records, **When** an administrator attempts to delete the branch, **Then** hard deletion is prohibited and the user is guided to deactivate the branch instead.
3. **Given** a Headquarters company with active operational branches, **When** an administrator attempts to deactivate the Headquarters, **Then** the system warns that all associated branches will also become inactive.

---

### User Story 3 - User Branch Access & Active Branch Context Switcher (Priority: P3)

Users within a tenant may only be permitted to view and record transactions for specific branches (e.g., an operator who only works at Store A vs. a financial manager who oversees all branches). When logged in, the user sees their current active branch in the top header and can switch between authorized branches.

**Why this priority**: Prevents cross-branch operational errors (e.g. issuing invoices or deducting stock from the wrong branch) and guarantees operational segregation.

**Independent Test**: Assign a user access to Branch 1 and Branch 2; verify the user can switch context between Branch 1 and Branch 2 in the UI, and verify they cannot see or select Branch 3.

**Acceptance Scenarios**:

1. **Given** a user authorized for Branch 1 and Branch 2, **When** they log into the system, **Then** their session defaults to their primary assigned branch and displays the branch switcher in the top navigation bar.
2. **Given** an active session, **When** the user selects Branch 2 in the branch switcher, **Then** the application context updates to Branch 2 without requiring re-authentication.
3. **Given** a user with restricted branch access, **When** requesting data for an unauthorized branch, **Then** the system blocks the request with an authorization error.

---

### User Story 4 - Dynamic Custom Fields for Companies & Branches (Priority: P4)

Businesses with industry-specific requirements need to capture custom metadata on companies and branches (e.g., SUFRAMA registration number, internal cost center code, refrigeration license, or regional supervisor).

**Why this priority**: Leverages the dynamic custom fields engine (Feature 005) to ensure companies and branches adapt flexibly without code modifications.

**Independent Test**: An administrator defines a custom field for the `COMPANY` entity (e.g., `suframa_code`), and verifies the field dynamically appears, validates, and persists in the company/branch form.

**Acceptance Scenarios**:

1. **Given** an active custom field defined for entity `COMPANY`, **When** an administrator creates or edits a company or branch, **Then** the custom field is rendered according to its configured type and validation rules.
2. **Given** custom field values saved for a company, **When** retrieving the company details, **Then** the custom values are returned and displayed accurately.

---

### Edge Cases

- **Duplicate CNPJ within Tenant**: System MUST reject registration of two entities (companies or branches) with identical CNPJ numbers under the same tenant.
- **CNPJ Check Digits**: Input containing 14 digits with incorrect check digits (Receita Federal algorithm) MUST be rejected before persistence.
- **Deactivating Headquarters with Active Filiais**: System MUST prevent or require explicit confirmation when deactivating a Headquarters that still has active branches.
- **Single Branch Tenant**: If a business only has one Headquarters and no branches, the context switcher should seamlessly display the single company without cluttering the UI.
- **IBGE City Code Validation**: The city selected in Brazilian addresses must map to a valid 7-digit IBGE code necessary for future electronic fiscal note (NF-e/NFS-e) generation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow tenant administrators with `COMPANIES_MANAGE` permission to create, view, update, and deactivate Companies and Branches.
- **FR-002**: System MUST validate the mathematical integrity of Brazilian CNPJ (14 digits) using the official modulo 11 algorithm.
- **FR-003**: System MUST enforce uniqueness of CNPJ within the scope of each tenant.
- **FR-004**: Each establishment record MUST capture:
  - Legal Name (Razão Social)
  - Trade Name (Nome Fantasia)
  - CNPJ (formatted `XX.XXX.XXX/XXXX-XX` and stored unformatted)
  - Inscrição Estadual (IE) and State Tax Exemption flag (`isExemptIE`)
  - Inscrição Municipal (IM)
  - Tax Regime (`SIMPLES_NACIONAL`, `SIMPLES_EXCESSO_SUBLIMITE`, `LUCRO_PRESUMIDO`, `LUCRO_REAL`)
  - CNAE Principal (7 digits) and optional secondary CNAEs
  - Establishment Type (`HEADQUARTERS` / Matriz or `BRANCH` / Filial)
  - Status (`ACTIVE` / `INACTIVE`)
- **FR-005**: System MUST record complete postal address for each company/branch:
  - Postal Code (CEP - 8 digits)
  - Street (Logradouro), Number, and Complement
  - Neighborhood (Bairro)
  - City (Município) and official 7-digit IBGE City Code (`codigoIbge`)
  - State (UF - 2 uppercase letters)
  - Country (País, defaulting to Brazil / BACEN code 1058)
- **FR-006**: System MUST record contact details for each establishment: phone number, operational email, and administrative contact person.
- **FR-007**: Operational branches (`BRANCH`) MUST be linked to an existing parent Headquarters (`HEADQUARTERS`) within the same tenant.
- **FR-008**: System MUST support associating users to one or more authorized branches, designating one as the default branch.
- **FR-009**: System MUST provide an active branch switcher in the frontend navigation header allowing authorized users to switch their working establishment context.
- **FR-010**: System MUST enforce multi-branch security: users without administrative global override cannot read or write data for branches they are not explicitly assigned to.
- **FR-011**: System MUST integrate `COMPANY` into the `CustomFieldEntityRegistry` allowing dynamic custom fields on companies and branches.
- **FR-012**: System MUST record immutable audit log entries for all create, update, and status-toggle operations on companies and branches.
- **FR-013**: System MUST provide dedicated access permissions: `COMPANIES_READ` and `COMPANIES_MANAGE`.
- **FR-014**: System MUST support internationalization (i18n) for all company and branch interfaces in `en-US` and `pt-BR`.

### Key Entities

- **Company / Branch (`CompanyBranch`)**: Represents a physical or legal establishment. Attributes: `id`, `tenantId`, `type` (`HEADQUARTERS` or `BRANCH`), `parentId` (nullable link to headquarters), `code` (e.g. `MATRIZ`, `FILIAL-01`), `legalName`, `tradeName`, `cnpj`, `stateRegistration` (IE), `isStateRegistrationExempt`, `municipalRegistration` (IM), `taxRegime`, `mainCnae`, `phone`, `email`, `address` (embedded/associated with CEP, street, number, complement, neighborhood, city, IBGE code, UF, country), `isActive`, `createdAt`, `updatedAt`.
- **UserBranchAccess**: Association entity defining which branches an `iam_user` is authorized to access, with an `isDefault` flag.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An administrator can successfully register a complete company or branch (including tax data and address) in under 90 seconds.
- **SC-002**: 100% of invalid CNPJ entries are intercepted and rejected with immediate descriptive feedback before hitting the database.
- **SC-003**: Active branch switching in the frontend completes instantaneously (under 200 milliseconds) without requiring user re-login.
- **SC-004**: Users are restricted to their assigned branches with 100% enforcement across all backend queries.
- **SC-005**: All company and branch actions generate traceable audit records matching the platform's audit compliance standard.

## Assumptions

- **Tenant Structure**: A single tenant represents a business group. A tenant can have one or more Headquarters companies, and each company can have multiple operational Branches.
- **CEP Auto-Complete**: The frontend can optionally integrate CEP lookup (e.g. ViaCEP or internal cache) for rapid address completion, while always permitting manual address entry.
- **Fiscal Readiness**: Gathering IBGE codes, CNAEs, and Tax Regime during company registration is mandatory to prepare the ERP for subsequent electronic invoicing (NF-e/NFS-e) modules.
- **Permissions**: System Administrator profiles automatically receive `COMPANIES_READ` and `COMPANIES_MANAGE` permissions.
