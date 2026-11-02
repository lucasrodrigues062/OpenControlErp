# Quickstart Validation Guide: User Management, Access Control (RBAC), and Audit Trail

**Target Feature**: [spec.md](spec.md) | **Branch**: `002-user-access-audit`

This guide walks through end-to-end scenarios to validate user management, single-profile assignment, granular permission evaluation, and the immutable audit trail across both the backend REST APIs and the React web frontend.

---

## Prerequisites

1. **Backend Running**:
   ```bash
   cd backend
   ./gradlew bootRun
   ```
   Backend listens on `http://localhost:8080`.

2. **Frontend Running**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Frontend development server listens on `http://localhost:5173`.

3. **Database**: PostgreSQL initialized with Flyway migrations up to `V1_0_5__create_iam_and_audit_tables.sql`.

---

## Scenario 1: Profile Creation & Permission Configuration

### Step 1.1: List Available System Permissions
```bash
curl -X GET http://localhost:8080/api/v1/iam/permissions \
  -H "Authorization: Bearer <ADMIN_JWT>"
```
**Expected Response (200 OK)**:
```json
[
  {
    "category": "User Management",
    "code": "USERS_READ",
    "label": "View Users",
    "description": "Allows listing and viewing user accounts"
  },
  {
    "category": "User Management",
    "code": "USERS_WRITE",
    "label": "Manage Users",
    "description": "Allows creating, updating, and deactivating users"
  },
  {
    "category": "Security",
    "code": "PROFILES_MANAGE",
    "label": "Manage Profiles",
    "description": "Allows creating and editing authorization profiles"
  },
  {
    "category": "Compliance",
    "code": "AUDIT_READ",
    "label": "View Audit Trail",
    "description": "Allows querying and inspecting audit log entries"
  }
]
```

### Step 1.2: Create a Custom Profile ("Sales Operator")
```bash
curl -X POST http://localhost:8080/api/v1/iam/profiles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -d '{
    "name": "Sales Operator",
    "description": "Standard operator profile with view access only",
    "permissions": ["USERS_READ"]
  }'
```
**Expected Response (201 Created)**:
Returns the created profile with a generated UUID and assigned permission `USERS_READ`.

---

## Scenario 2: User Registration & Single-Profile Association

### Step 2.1: Create a New User
```bash
curl -X POST http://localhost:8080/api/v1/iam/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -d '{
    "username": "carlos.silva",
    "email": "carlos.silva@opencontrol.org",
    "password": "Password123!",
    "fullName": "Carlos Silva",
    "profileId": "<SALES_OPERATOR_PROFILE_UUID>"
  }'
```
**Expected Response (201 Created)**:
User is created with status `ACTIVE` and `profile.name` set to `"Sales Operator"`.

---

## Scenario 3: Granular Authorization Enforcement

### Step 3.1: Authenticate as Carlos Silva
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "carlos.silva",
    "password": "Password123!"
  }'
```
**Expected Response (200 OK)**:
Returns JWT access token containing permissions inherited from "Sales Operator" (`USERS_READ`).

### Step 3.2: Verify Allowed Operation
```bash
curl -X GET http://localhost:8080/api/v1/iam/users \
  -H "Authorization: Bearer <CARLOS_JWT>"
```
**Expected Outcome**: 200 OK (Carlos has `USERS_READ`).

### Step 3.3: Verify Denied Operation (Profile Lacks `USERS_WRITE`)
```bash
curl -X POST http://localhost:8080/api/v1/iam/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <CARLOS_JWT>" \
  -d '{
    "username": "unauthorized.user",
    "email": "unauth@opencontrol.org",
    "password": "Password123!",
    "fullName": "Unauthorized",
    "profileId": "<PROFILE_UUID>"
  }'
```
**Expected Outcome**: 403 Forbidden with ProblemDetail explaining the missing permission.

---

## Scenario 4: Automated Audit Trail Verification (Who, What, When)

### Step 4.1: Query Audit Trail for User Creation
```bash
curl -X GET "http://localhost:8080/api/v1/audit/logs?entityType=User&action=CREATE" \
  -H "Authorization: Bearer <ADMIN_JWT>"
```
**Expected Response (200 OK)**:
```json
{
  "items": [
    {
      "actorUsername": "admin",
      "action": "CREATE",
      "entityType": "User",
      "entityId": "<CARLOS_UUID>",
      "details": "Created user 'carlos.silva' with profile 'Sales Operator'",
      "stateBefore": null,
      "stateAfter": {
        "username": "carlos.silva",
        "email": "carlos.silva@opencontrol.org",
        "status": "ACTIVE"
      },
      "createdAt": "2026-11-02T10:15:30Z"
    }
  ]
}
```

---

## Scenario 5: Web Frontend Interactive Walkthrough

1. Open `http://localhost:5173` in a browser.
2. Log in with `admin` / `admin123`.
3. Navigate to **Profiles**:
   - Click **New Profile**, name it "Auditor", toggle `AUDIT_READ`, and click **Save**.
4. Navigate to **Users**:
   - Click **New User**, enter name, email, and choose "Auditor" from the single-profile select dropdown.
   - Observe immediate addition to the user table.
5. Navigate to **Audit Trail**:
   - Inspect the real-time table displaying the creation events of both the "Auditor" profile and the new user.
   - Click the row to open the lateral drawer displaying the formatted before/after JSON diff.
