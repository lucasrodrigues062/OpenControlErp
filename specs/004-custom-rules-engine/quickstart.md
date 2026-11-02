# Quickstart: Custom Business Rules Engine

**Feature**: `004-custom-rules-engine`  
**Purpose**: Runnable validation scenarios demonstrating authoring, dry-run simulation, execution interception, strict vs permissive failure handling, and execution tracing.

---

## Scenario 1: Authoring & Dry-Run Simulation (Visual Condition Builder)

Validate authoring a discount ceiling rule via Visual Builder and testing it with simulation before enabling.

1. Navigate to `http://localhost:5173/rules` and click **"Create Rule"** ("Criar Regra").
2. Fill the rule header:
   - **Name**: `Max Wholesale Discount 15%`
   - **Target Entity**: `SALE`
   - **Trigger Point**: `SALE_FINALIZED`
   - **Evaluation Timing**: `BEFORE`
   - **Failure Policy**: `ABORT_TRANSACTION`
3. In the **Visual Builder**, configure:
   - Field: `discountPercentage`
   - Operator: `GREATER_THAN`
   - Value: `15.0`
4. Action:
   - Action Type: `BLOCK`
   - Action Value: `{"message": "Wholesale discount cannot exceed 15% without director approval."}`
5. Open the **"Dry-Run Simulation"** panel:
   - Provide Context JSON:
     ```json
     {
       "sale": {
         "id": "11111111-1111-1111-1111-111111111111",
         "totalAmount": 1200.0,
         "discountPercentage": 20.0
       }
     }
     ```
   - Click **"Simulate Rule"** ("Simular Regra").
   - Response verifies: `matched: true`, `action: "BLOCK"`, `simulatedError: "Wholesale discount cannot exceed 15% without director approval."`, `executionTimeMs: < 2ms`.
6. Save the rule. Verify it appears active in the rules table.

---

## Scenario 2: Synchronous Transaction Interception & Rollback (Strict Mode)

Verify that an active `BEFORE` rule with `ABORT_TRANSACTION` blocks a business transaction in the backend.

1. Submit a sale finalization request with `discountPercentage: 18.0`:
   ```http
   POST /api/v1/sales/order-123/finalize
   Content-Type: application/json
   X-Tenant-Id: default-tenant

   {
     "discountPercentage": 18.0,
     "totalAmount": 5000.0
   }
   ```
2. The `RuleExecutionInterceptor` evaluates the rule against the domain context:
   - Rule condition matches (`18.0 > 15.0`).
   - Action `BLOCK` triggers `BusinessRuleViolationException`.
3. System responds with HTTP `422 Unprocessable Entity`:
   ```json
   {
     "status": 422,
     "error": "Business Rule Violation",
     "ruleName": "Max Wholesale Discount 15%",
     "message": "Wholesale discount cannot exceed 15% without director approval."
   }
   ```
4. Verify database: The sale remains in its previous state; no finalized order or inventory change is committed.
5. In Traces view (`/rules/traces`), verify an entry with status `BLOCKED` and execution duration `< 3ms`.

---

## Scenario 3: Advanced SpEL Expression & Field Mutation

Verify that an Advanced DSL rule can evaluate complex SpEL logic and mutate target entity fields before commit.

1. Create a rule in **Advanced DSL mode**:
   - **Name**: `Auto-Apply VIP Customer Tag & Free Shipping`
   - **Target Entity**: `SALE`
   - **Trigger Point**: `SALE_FINALIZED`
   - **Evaluation Timing**: `BEFORE`
   - **Expression DSL**: `#sale.customer.tier == 'VIP' and #sale.totalAmount >= 500.0`
   - **Action Type**: `SET_FIELD`
   - **Action Value**: `{"field": "shippingFee", "value": 0.0}`
2. Execute a dry-run test:
   - Provide Context JSON:
     ```json
     {
       "sale": {
         "customer": { "tier": "VIP" },
         "totalAmount": 750.0,
         "shippingFee": 45.0
       }
     }
     ```
   - Simulation verifies: `matched: true`, mutated `shippingFee: 0.0`.
3. Save the rule.
4. Finalize a VIP sale of $750.0; inspect resulting sale record: `shippingFee` is persisted as `0.0`.

---

## Scenario 4: Permissive Failure Policy (`LOG_AND_CONTINUE`)

Verify resilience when a non-critical notification rule fails evaluation or calculation.

1. Create a rule:
   - **Name**: `Notify Partner Webhook`
   - **Trigger Point**: `SALE_FINALIZED`
   - **Evaluation Timing**: `AFTER`
   - **Failure Policy**: `LOG_AND_CONTINUE`
   - **Expression DSL**: `#unknownHelper.calculatePoints(#sale)` (Intentionally faulty expression)
2. Finalize a sale in the system:
   - The sale finalization completes successfully (HTTP `200 OK`).
3. Check `rule_execution_trace` table or UI trace viewer:
   - Trace status: `ERROR`
   - Failure policy: `LOG_AND_CONTINUE`
   - Error message details SpEL property resolution error.
   - Core transaction was not rolled back.
