# Quickstart & Verification Guide: Company & Branch Management

**Feature**: `006-company-branches`
**Date**: 2026-11-02

## Overview

This guide walks through validating the Company and Branch Management capability end-to-end:
1. Creating a primary Headquarters company (`HEADQUARTERS` / Matriz) with statutory Brazilian fiscal fields.
2. Registering an operational branch (`BRANCH` / Filial) linked to the Headquarters.
3. Updating fiscal parameters and accountant details.
4. Simulating A1 Digital Certificate upload and validation.
5. Verifying user branch assignment and top navigation active branch switcher.

---

## Prerequisites

1. PostgreSQL database running with Flyway migration `V1_0_9__create_company_branches_tables.sql` applied.
2. Authenticated user with `COMPANIES_MANAGE` and `COMPANIES_READ` permissions (e.g., standard `ADMIN` profile).
3. Valid bearer token in the `Authorization: Bearer <TOKEN>` header.

---

## Scenario 1: Create Headquarters Company (Matriz)

### Request
```http
POST /api/v1/companies
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "type": "HEADQUARTERS",
  "code": "01",
  "tradeName": "OpenControl Matriz SP",
  "legalName": "OpenControl Tecnologia e Gestao Empresarial LTDA",
  "cnpj": "11222333000181",
  "stateRegistration": "111222333444",
  "isStateRegistrationExempt": false,
  "municipalRegistration": "98765432",
  "mainCnae": "6202300",
  "phone1": "(11) 3344-5566",
  "email": "contato@opencontrol.com.br",
  "responsiblePerson": "Lucas Rodrigues",
  "address": {
    "postalCode": "01310100",
    "street": "Avenida Paulista",
    "number": "1000",
    "complement": "Sala 101",
    "neighborhood": "Bela Vista",
    "cityName": "Sao Paulo",
    "cityIbgeCode": 3550308,
    "stateCode": "SP",
    "countryCode": "BRA"
  }
}
```

### Expected Response (`201 Created`)
- Response contains generated `id`, `type: "HEADQUARTERS"`, and `isActive: true`.
- Record exists in `company_branches` and `branch_fiscal_settings` is automatically initialized with defaults.

---

## Scenario 2: Create Operational Branch (Filial)

### Request
```http
POST /api/v1/companies
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "type": "BRANCH",
  "parentId": "<HEADQUARTERS_UUID>",
  "code": "02",
  "tradeName": "OpenControl Filial Campinas",
  "legalName": "OpenControl Tecnologia e Gestao Empresarial LTDA",
  "cnpj": "11222333000262",
  "stateRegistration": "555666777888",
  "isStateRegistrationExempt": false,
  "municipalRegistration": "12345678",
  "mainCnae": "6202300",
  "phone1": "(19) 3322-1100",
  "email": "campinas@opencontrol.com.br",
  "responsiblePerson": "Carlos Silva",
  "address": {
    "postalCode": "13010000",
    "street": "Rua Barao de Jaguara",
    "number": "500",
    "neighborhood": "Centro",
    "cityName": "Campinas",
    "cityIbgeCode": 3509502,
    "stateCode": "SP",
    "countryCode": "BRA"
  }
}
```

### Expected Response (`201 Created`)
- Response contains `parentId` referencing the Headquarters and status `isActive: true`.

---

## Scenario 3: Update Fiscal Parameters & Accountant

### Request
```http
PUT /api/v1/companies/<BRANCH_UUID>/fiscal-settings
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "taxRegime": "SIMPLES_NACIONAL",
  "pisCofinsRegime": "CUMULATIVO",
  "isTaxSubstitute": false,
  "isIpiTaxpayer": false,
  "calculateFcp": false,
  "deductIcmsFromPiscofinsBasis": true,
  "icmsSnCreditRate": 2.500,
  "issRate": 5.000,
  "cbsRate": 0.000,
  "nfeEnvironment": "HOMOLOGATION",
  "nfeSeries": "1",
  "nfeModel": "55"
}
```

### Expected Response (`200 OK`)
- `branch_fiscal_settings` updated with provided values and audit event recorded.

---

## Scenario 4: User Branch Listing & Context Switch

### Request
```http
GET /api/v1/companies/user-branches
Authorization: Bearer <TOKEN>
```

### Expected Response (`200 OK`)
```json
{
  "activeBranchId": "<HEADQUARTERS_UUID>",
  "branches": [
    {
      "id": "<HEADQUARTERS_UUID>",
      "type": "HEADQUARTERS",
      "code": "01",
      "tradeName": "OpenControl Matriz SP",
      "cnpj": "11222333000181",
      "isActive": true
    },
    {
      "id": "<BRANCH_UUID>",
      "type": "BRANCH",
      "code": "02",
      "tradeName": "OpenControl Filial Campinas",
      "cnpj": "11222333000262",
      "isActive": true
    }
  ]
}
```

---

## Automated Verification

Run backend unit and integration test suite:
```bash
./gradlew test
```

Run frontend test suite:
```bash
npm --prefix frontend test -- --run
```
