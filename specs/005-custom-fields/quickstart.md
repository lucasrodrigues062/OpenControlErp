# Quickstart: Dynamic Custom Fields Engine Validation Guide

**Feature**: `005-custom-fields`
**Date**: 2026-11-02

This guide outlines end-to-end scenarios to verify that the Custom Fields Engine functions properly across definition management, value persistence, declarative validation, and UI integration.

## Prerequisites

1. PostgreSQL database running and Flyway migrations applied (`V1_0_8__create_custom_fields_tables.sql`).
2. Backend running on `http://localhost:8080`.
3. Frontend running on `http://localhost:5173`.
4. Authenticated administrative session with tenant identifier in security context.

---

## Scenario 1: Define Custom Fields for Customer Entity

### Goal
Configure a required Text field (`spouse_name`) and a Select dropdown (`customer_tier`) on the `CUSTOMER` entity.

### Steps
1. Send `POST /api/v1/custom-fields/definitions`:
   ```bash
   curl -X POST http://localhost:8080/api/v1/custom-fields/definitions \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" \
     -d '{
       "targetEntity": "CUSTOMER",
       "fieldKey": "spouse_name",
       "label": "Spouse Name",
       "fieldType": "TEXT",
       "required": true,
       "adminOnly": false,
       "readOnly": false,
       "displayOrder": 1
     }'
   ```
2. Send `POST /api/v1/custom-fields/definitions` for the dropdown:
   ```bash
   curl -X POST http://localhost:8080/api/v1/custom-fields/definitions \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" \
     -d '{
       "targetEntity": "CUSTOMER",
       "fieldKey": "customer_tier",
       "label": "Customer Tier",
       "fieldType": "SELECT",
       "required": false,
       "options": ["BRONZE", "SILVER", "GOLD", "PLATINUM"],
       "displayOrder": 2
     }'
   ```

### Expected Outcome
- Both definitions return HTTP 201 Created with distinct UUIDs and matching metadata.
- Requesting `GET /api/v1/custom-fields/definitions?targetEntity=CUSTOMER` returns both fields ordered by `displayOrder`.

---

## Scenario 2: Save and Validate Custom Field Values on Entity Record

### Goal
Submit valid custom field values for a specific Customer record and verify validation enforcement on invalid inputs.

### Steps
1. **Invalid Option Test**: Submit an unlisted option for `customer_tier`:
   ```bash
   curl -X PUT http://localhost:8080/api/v1/custom-fields/values/CUSTOMER/$CUSTOMER_ID \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" \
     -d '{
       "values": {
         "spouse_name": "Mary Jane",
         "customer_tier": "DIAMOND"
       }
     }'
   ```
   *Expected Outcome*: HTTP 400 Bad Request indicating `"DIAMOND is not a valid option for customer_tier"`.

2. **Missing Required Field Test**: Submit payload omitting `spouse_name`:
   ```bash
   curl -X PUT http://localhost:8080/api/v1/custom-fields/values/CUSTOMER/$CUSTOMER_ID \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" \
     -d '{
       "values": {
         "customer_tier": "GOLD"
       }
     }'
   ```
   *Expected Outcome*: HTTP 400 Bad Request indicating required field `spouse_name` is missing.

3. **Valid Payload**:
   ```bash
   curl -X PUT http://localhost:8080/api/v1/custom-fields/values/CUSTOMER/$CUSTOMER_ID \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer $TOKEN" \
     -d '{
       "values": {
         "spouse_name": "Mary Jane",
         "customer_tier": "GOLD"
       }
     }'
   ```
   *Expected Outcome*: HTTP 200 OK returning persisted field values.

4. **Retrieve Values**:
   ```bash
   curl http://localhost:8080/api/v1/custom-fields/values/CUSTOMER/$CUSTOMER_ID \
     -H "Authorization: Bearer $TOKEN"
   ```
   *Expected Outcome*: Returns `{"spouse_name": "Mary Jane", "customer_tier": "GOLD"}`.

---

## Scenario 3: Verify Role-Based Field Flags (Admin-Only & Read-Only)

### Goal
Confirm that `adminOnly` fields cannot be viewed or modified by non-administrators, and `readOnly` fields cannot be altered after initial creation.

### Steps
1. Create field `internal_credit_rating` with `adminOnly: true`.
2. As an operator without admin roles, request `GET /api/v1/custom-fields/values/CUSTOMER/$CUSTOMER_ID`.
   *Expected Outcome*: The response omits `internal_credit_rating`.
3. Create field `initial_registration_branch` with `readOnly: true`.
4. Create record with `initial_registration_branch: "SP-01"`.
5. Update record attempting to change `initial_registration_branch: "RJ-02"`.
   *Expected Outcome*: HTTP 400 Bad Request / 403 Forbidden with message indicating immutable field cannot be modified.

---

## Scenario 4: Dynamic UI Integration

### Goal
Verify in the browser that the Custom Fields administrative interface and entity form sections render seamlessly.

### Steps
1. Navigate to `/settings/custom-fields`.
2. Select the **Customer** tab.
3. Observe configured custom fields (`Spouse Name`, `Customer Tier`), toggle active status, and reorder fields via drag-and-drop or order arrows.
4. Open the Customer creation form and verify that `CustomFieldFormSection` displays:
   - A text input for "Spouse Name" marked as required (*).
   - A select dropdown for "Customer Tier" containing Bronze, Silver, Gold, Platinum.
5. Fill valid data, save the entity, and verify that the detail card renders values with proper localized labels.
