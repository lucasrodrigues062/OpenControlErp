# Quickstart & Verification Guide: CRM, Leads & Sales Pipeline

**Feature Branch**: `007-crm-sales-pipeline`  
**Date**: 2026-11-02  
**Spec Reference**: `specs/007-crm-sales-pipeline/spec.md`  
**API Reference**: `specs/007-crm-sales-pipeline/contracts/crm-api.yaml`

This guide specifies runnable validation scenarios verifying all 4 User Stories end-to-end across backend and frontend.

---

## 1. Prerequisites & Environment Setup

1. **Active Database & Migrations**:
   Ensure PostgreSQL is running and migrations through `V1_0_10__create_crm_tables.sql` are applied.
   ```bash
   # Verify backend build and migration
   cd backend
   ./gradlew test --tests "*Crm*"
   ```
2. **Frontend Dev Server**:
   ```bash
   cd frontend
   npm run dev
   # Accessible at http://localhost:5173
   ```
3. **Authentication Token**:
   Log in with an administrator account (`admin` / password) to obtain a JWT token:
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"password123"}'
   ```

---

## 2. Validation Scenario 1: Customer Registration, Validation & Categorization (User Story 1)

### Objective
Verify that Legal Entities (PJ) and Individuals (PF) are registered with Brazilian Modulo 11 document validation, assigned to Customer Categories and Economic Groups, and queryable with proper masking.

### Step 1: Create a Customer Category and Economic Group
```bash
# 1. Create Category "Atacado"
curl -X POST http://localhost:8080/api/v1/crm/customer-categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "ATACADO",
    "name": "Clientes Atacadistas",
    "colorHex": "#3B82F6",
    "defaultDiscountPct": 10.00
  }'

# 2. Create Economic Group "Grupo Sim"
curl -X POST http://localhost:8080/api/v1/crm/economic-groups \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "GRP-SIM",
    "name": "Holding Sim Tecnologia",
    "holdingCnpj": "53066208000170",
    "consolidatedCreditLimit": 200000.00
  }'
```

### Step 2: Register a Legal Entity (PJ) with CNPJ Validation
```bash
curl -X POST http://localhost:8080/api/v1/crm/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "LEGAL_ENTITY",
    "document": "53066208000170",
    "legalName": "Sym Soluções em Tecnologia Ltda",
    "tradeName": "Sym Tech",
    "stateRegistration": "123456789",
    "isStateRegistrationExempt": false,
    "icmsTaxpayerType": "CONTRIBUTOR",
    "phone1": "11988887777",
    "email": "contato@symtech.com.br",
    "creditLimit": 50000.00,
    "categoryId": "<CATEGORY_UUID>",
    "economicGroupId": "<GROUP_UUID>",
    "postalCode": "01310100",
    "street": "Avenida Paulista",
    "number": "1000",
    "neighborhood": "Bela Vista",
    "cityName": "São Paulo",
    "stateCode": "SP"
  }'
```
**Expected Outcome**: Returns HTTP `201 Created` with formatted document `53.066.208/0001-70`, `creditStatus: "APPROVED"`, and linked category/group details.

### Step 3: Verify Invalid Modulo 11 Rejection
```bash
curl -X POST http://localhost:8080/api/v1/crm/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "LEGAL_ENTITY",
    "document": "11111111111111",
    "legalName": "Empresa Falsa"
  }'
```
**Expected Outcome**: Returns HTTP `400 Bad Request` with message `Invalid CNPJ check digits (Modulo 11)`.

---

## 3. Validation Scenario 2: Lead Capture, Qualification & 1-Click Conversion (User Story 2)

### Objective
Capture an inbound prospect lead, move through qualification stages, disqualify with reason or convert directly into an active Customer and Opportunity in the sales pipeline.

### Step 1: Create a Lead
```bash
curl -X POST http://localhost:8080/api/v1/crm/leads \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Roberto Mendes",
    "companyName": "Mendes Distribuidora",
    "email": "roberto@mendesdistribuidora.com.br",
    "phone": "11999990000",
    "source": "WEBSITE",
    "estimatedBudget": 35000.00,
    "interestSummary": "Interesse no ERP para 3 filiais"
  }'
```
**Expected Outcome**: Returns HTTP `201 Created` with `status: "NEW"`.

### Step 2: One-Click Convert Lead to Customer and Deal
```bash
curl -X POST http://localhost:8080/api/v1/crm/leads/<LEAD_UUID>/convert \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerType": "LEGAL_ENTITY",
    "customerDocument": "04252011000110",
    "customerLegalName": "Mendes Distribuidora de Alimentos Ltda",
    "dealTitle": "Implantação ERP Mendes",
    "dealEstimatedValue": 35000.00,
    "pipelineId": "<PIPELINE_UUID>",
    "stageId": "<FIRST_STAGE_UUID>"
  }'
```
**Expected Outcome**:
1. Returns HTTP `200 OK` with `{ leadId, customerId, dealId }`.
2. Lead status becomes `CONVERTED`.
3. New Customer is created with CNPJ `04.252.011/0001-10`.
4. New Deal is initialized in the target pipeline stage with value `35000.00`.

---

## 4. Validation Scenario 3: Sales Pipeline & Visual Kanban Drag-and-Drop (User Story 3)

### Objective
Test default sales pipeline creation, deal cards movement across stages, real-time value recalculation, and mandatory loss reason capture when dropped into `CLOSED_LOST`.

### Step 1: Query Pipeline Metrics
```bash
curl -X GET "http://localhost:8080/api/v1/crm/deals/metrics?pipelineId=<PIPELINE_UUID>" \
  -H "Authorization: Bearer $TOKEN"
```
**Expected Outcome**: Returns aggregated metrics including `totalOpenDeals`, `totalPipelineValue`, `weightedPipelineValue`, and `winRatePct`.

### Step 2: Drag & Drop Deal to Intermediate Stage
```bash
curl -X PATCH http://localhost:8080/api/v1/crm/deals/<DEAL_UUID>/stage \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "stageId": "<PROPOSAL_STAGE_UUID>"
  }'
```
**Expected Outcome**: HTTP `200 OK`, deal `stageId` updated, timeline activity recorded.

### Step 3: Move Deal to Closed Lost (Testing Loss Reason Requirement)
```bash
# 1. Attempt without lossReason:
curl -X PATCH http://localhost:8080/api/v1/crm/deals/<DEAL_UUID>/stage \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "stageId": "<LOST_STAGE_UUID>"
  }'
# Expected Outcome: HTTP 400 Bad Request ("lossReason is required when marking deal as lost")

# 2. Provide lossReason:
curl -X PATCH http://localhost:8080/api/v1/crm/deals/<DEAL_UUID>/stage \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "stageId": "<LOST_STAGE_UUID>",
    "lossReason": "Preço fora do orçamento do cliente"
  }'
# Expected Outcome: HTTP 200 OK, status: "LOST", closedAt set.
```

---

## 5. Validation Scenario 4: Activity Timeline & Dynamic Custom Fields (User Story 4)

### Objective
Verify chronological logging of calls, WhatsApp notes, and meetings, alongside integration with the dynamic Custom Fields engine on the Customer entity.

### Step 1: Log an Activity
```bash
curl -X POST http://localhost:8080/api/v1/crm/activities \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "CUSTOMER",
    "entityId": "<CUSTOMER_UUID>",
    "activityType": "WHATSAPP",
    "summary": "Enviada tabela de preços atualizada pelo WhatsApp",
    "description": "Cliente solicitou condição com 28 dias de prazo para primeiro pedido."
  }'
```

### Step 2: Verify Timeline Query
```bash
curl -X GET "http://localhost:8080/api/v1/crm/activities?entityType=CUSTOMER&entityId=<CUSTOMER_UUID>" \
  -H "Authorization: Bearer $TOKEN"
```
**Expected Outcome**: Returns activities sorted descending by `performedAt`, showing activity type badge, summary, and author.

### Step 3: Custom Field Verification
1. Navigate to Settings -> Custom Fields -> create a custom field for entity `CUSTOMER` (e.g. `regime_tributario_especial` or `fiador_nome`).
2. Open Customer Detail page in frontend.
3. Verify that the custom field renders and persists values seamlessly.

---

## 6. Automated Test Suite Execution
Execute the full automated test suite to confirm $\ge 90\%$ test coverage:
```bash
# Backend unit & integration test coverage gate
cd backend
./gradlew test jacocoTestReport jacocoTestCoverageVerification

# Frontend test suite
cd ../frontend
npm run test:run
```
