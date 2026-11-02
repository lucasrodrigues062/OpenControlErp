# Quickstart Validation Guide: Extension Engine & Event Bus

This guide details end-to-end validation scenarios for verifying the Extension Engine, SPI resolver, and Event Bus.

## Prerequisites

- **Java Development Kit**: JDK 21 (LTS)
- **Database**: PostgreSQL 16+ running locally or in Docker container
- **API Tool**: `curl`, HTTPie, or Postman

---

## 1. Setup & Environment Initialization

1. Start PostgreSQL instance:
   ```bash
   docker run --name opencontrol-db -e POSTGRES_DB=opencontrol -e POSTGRES_USER=opencontrol -e POSTGRES_PASSWORD=opencontrol -p 5432:5432 -d postgres:16
   ```

2. Run the application (or integration test harness):
   ```bash
   ./gradlew test --tests ExtensionEngineIntegrationTest
   ```

---

## 2. Validation Scenario 1: Plugin Lifecycle (Install -> Activate -> Deactivate)

### Step 2.1: Upload and Install Plugin Package
Submit a plugin archive (e.g., sample plugin with manifest) via REST API:

```bash
curl -X POST http://localhost:8080/api/v1/extensions \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -F "file=@sample-brazil-fiscal-plugin-1.0.0.jar"
```

**Expected Outcome**:
- HTTP `201 Created`
- Response payload displays `status: "INSTALLED"`
- Database table `extension_plugin` contains the record
- Database table `extension_audit_log` records an `INSTALL` event

### Step 2.2: Activate the Plugin
```bash
curl -X POST http://localhost:8080/api/v1/extensions/{PLUGIN_UUID}/activate \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Expected Outcome**:
- HTTP `200 OK`
- Response displays `status: "ACTIVE"`
- SPI implementations are registered in the runtime registry
- Database table `extension_audit_log` records an `ACTIVATE` event

### Step 2.3: Deactivate the Plugin
```bash
curl -X POST http://localhost:8080/api/v1/extensions/{PLUGIN_UUID}/deactivate \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Expected Outcome**:
- HTTP `200 OK`
- Response displays `status: "INACTIVE"`
- SPI registry unbinds the plugin's providers, restoring core default fallbacks

---

## 3. Validation Scenario 2: Contextual SPI Resolution

Verify that invoking an SPI resolves the active extension's provider according to context.

1. Issue a test business transaction with context `country: "BR"`:
   - When the Brazil fiscal plugin is **ACTIVE**, the resolved provider is `BrazilianFiscalEngineAdapter`.
   - When the plugin is **INACTIVE**, the system resolves `DefaultNoOpFiscalEngine` without raising errors or crashing the transaction.

---

## 4. Validation Scenario 3: Transactional Event Bus & Outbox Dispatch

Verify synchronous pre-commit validation and asynchronous post-commit outbox durability.

1. **Pre-Commit Interceptor Validation**:
   - Trigger a business action (e.g., invoice issuance).
   - An active pre-commit plugin listener throwing a `ValidationException` successfully aborts the transaction and rolls back database state.

2. **Post-Commit Asynchronous Reliability**:
   - Trigger a successful business action.
   - An entry is inserted into `event_outbox` with status `PENDING`.
   - The asynchronous worker picks up the event, invokes registered active plugin subscribers, and updates status to `PUBLISHED`.
   - Simulate a listener exception: verify status transitions to `FAILED` with incremented `retry_count`, and subsequent retries are executed using exponential backoff.
