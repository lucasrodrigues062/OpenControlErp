# Feature Specification: Dynamic Custom Business Rules Engine

**Feature Branch**: `004-custom-rules-engine`

**Created**: 2026-11-02

**Status**: Ready

**Input**: User description: "Agora pensa, que o erp vai ser usada por diferentes empresas, de diferentes tipos, de diferentes segmentos com diversas particularidades. Fazer um parametro ou uma regra para cada uma seria impossivel de manter. O que pensei algo como Custom Rules, Regras customizadas. Ela pode ser escrita em uma DSL, ou proprio SQL mesmo. cada regra teria um trigger, por exemplo, ao fim da venda, ao inserir um produto na venda, ao baixar um financeiro ou ao criar, ao criar o produto, ao editar um produto etc.. Aceito sugestões de melhor maneiras de resolver esse problema"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create and Manage Tenant-Specific Business Rules (Priority: P1) 🎯 MVP

As an administrator or business consultant representing a specific company/tenant, I want to define custom rules linked to operational lifecycle events (e.g., item added to sale, sale finalized, product created/updated, financial receivable settled) using either a Visual No-Code Builder or an Advanced Expression DSL, so that the ERP adapts to our specific segment processes without source code alterations or endless hardcoded configuration parameters.

**Why this priority**: Solves the fundamental scaling and multi-segment customization challenge of the ERP without hardcoding endless parameter toggles.

**Independent Test**:
Configure a rule for the event `sale.item_added` with condition `item.discount_percentage > 20.0` that aborts the action with a customized error message ("Desconto acima de 20% requer aprovação gerencial"). Trigger an item insertion with 25% discount and verify immediate rejection; trigger with 15% and verify acceptance.

**Acceptance Scenarios**:
1. **Given** an active custom validation rule on `sale.finalize` requiring customer document for sales above $1,000, **When** a user attempts to finalize a sale of $1,500 without document, **Then** the transaction is blocked with the rule's custom message.
2. **Given** an active transformation rule on `product.create`, **When** a product is saved with empty SKU, **Then** the rule generates and assigns a structured SKU according to the rule formula before persisting.
3. **Given** a rule created via the Visual No-Code Builder, **When** saved and executed, **Then** the condition criteria and action templates execute with the same fidelity as the Advanced DSL.
4. **Given** two distinct tenants in the ERP, **When** Tenant A configures a custom rule, **Then** Tenant B's operations are completely unaffected and isolated.

---

### User Story 2 - Execution Engine with Sandboxing, Timeouts, and Policy-Based Error Handling (Priority: P1)

As a system operator, I want all custom rules to execute within a strictly sandboxed environment with CPU/memory limits, timeout bounds, and configurable error handling policies (Strict vs Permissive), so that a faulty user rule cannot crash the server, freeze a database transaction, or compromise operations.

**Why this priority**: Security and reliability guarantee. In an enterprise multi-tenant ERP, executing dynamic code or user-provided logic must never compromise platform stability.

**Independent Test**:
Create a rule configured with `error_handling_policy: LOG_AND_CONTINUE` that triggers a divide-by-zero runtime error. Execute the operation and verify the transaction succeeds while an error trace is recorded. Change policy to `ABORT_TRANSACTION` and verify the transaction is blocked.

**Acceptance Scenarios**:
1. **Given** a rule that exceeds execution timeout threshold (e.g., 200ms), **When** evaluated, **Then** the engine cancels execution, logs a timeout warning, and follows the configured error handling policy.
2. **Given** a rule configured with `ABORT_TRANSACTION`, **When** a runtime expression evaluation fails, **Then** the transaction rolls back cleanly with a descriptive error.
3. **Given** a rule configured with `LOG_AND_CONTINUE`, **When** a runtime expression evaluation fails, **Then** the error is logged to `rule_execution_trace` and the transaction proceeds normally.
4. **Given** a rule attempting to access restricted Java classes, system environment variables, or disk files, **When** compiled/evaluated, **Then** the security sandbox blocks the execution before runtime.

---

### User Story 3 - Visual No-Code Builder & Interactive Test Simulation (Priority: P2)

As a business consultant or non-developer administrator, I want to assemble rules using visual dropdowns, operators, and action templates (with the option to switch to an Advanced Expression DSL tab), and dry-run test them with sample JSON payloads, so that I can verify rule behavior before putting it into production.

**Why this priority**: Democratizes rule authoring for business analysts and consultants while minimizing deployment errors.

**Independent Test**:
Open the Rule Editor in the web interface, select trigger `sale.item_added`, build visual condition `item.unit_price < 10.00`, set action `BLOCK_OPERATION("Preço unitário abaixo do mínimo permitido")`, click "Test Rule" with a mock payload `{ unit_price: 8.50 }`, and verify the simulation displays the expected block response.

**Acceptance Scenarios**:
1. **Given** the rule editor, **When** the user selects Visual Mode, **Then** available entity fields, operators (`EQUALS`, `GREATER_THAN`, `LESS_THAN`, `IN`, `CONTAINS`), and action templates are presented as guided dropdowns.
2. **Given** the rule editor, **When** the user toggles to Advanced DSL Mode, **Then** a code editor with syntax checking and MVEL/SpEL expression capability is provided.
3. **Given** any rule in the editor, **When** the user submits a sample payload in the Simulation panel, **Then** the engine dry-runs the rule without persisting or modifying live data and returns the evaluation outcome and execution latency.

---

### User Story 4 - Rule Execution Audit and Traceability (Priority: P2)

As a business auditor or administrator, I want to inspect execution logs of triggered custom rules, including evaluation result, execution duration, and state modifications, so that we can troubleshoot why a specific rule fired or failed.

**Why this priority**: Full observability for business rules, allowing consultants to debug logic without developer intervention.

**Independent Test**:
Execute an operation triggering 3 rules, navigate to the Rules Execution Log, and verify a chronological trace displaying which rules evaluated to true, what actions were applied, and latency in milliseconds.

**Acceptance Scenarios**:
1. **Given** a triggered event, **When** rules are evaluated, **Then** an execution summary is recorded containing rule ID, event, execution time, result (`MATCHED`, `SKIPPED`, `ERROR`), and message.

---

### Edge Cases

- **Circular Rule Triggers**: Rule on `product.update` triggers a mutation that could fire `product.update` again. System must prevent recursive cascading execution via maximum call depth limit (depth limit = 1).
- **Rule Syntax Error during Runtime**: If an enabled rule contains a runtime error (e.g., dividing by zero or referencing null property), the engine logs the failure and falls back safely according to rule error-handling policy (`ABORT_TRANSACTION` vs `LOG_AND_CONTINUE`).
- **High-Volume Transaction Latency**: Rules evaluated synchronously in high-throughput operations (e.g., adding items to POS sales) must evaluate in `< 5ms`.
- **Database Transaction Rollback**: If a synchronous `BEFORE` validation rule fails, the entire transaction rolls back cleanly without orphan state.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a unified trigger catalog defining supported lifecycle hook points across ERP domains:
  - Sales: `sale.created`, `sale.item_added`, `sale.item_removed`, `sale.finalized`, `sale.cancelled`.
  - Products/Inventory: `product.created`, `product.updated`, `stock.movement_recorded`.
  - Financial: `financial.title_created`, `financial.title_settled`, `financial.title_cancelled`.
- **FR-002**: System MUST support execution timing modes:
  - `BEFORE` (Synchronous Pre-Commit): Intercepts the operation with read/write access to entity payload; can modify data or abort the transaction.
  - `AFTER` (Asynchronous Post-Commit): Non-blocking side-effects executed after database commit (notifications, webhooks, audit).
- **FR-003**: System MUST support a **Hybrid Rule Authoring Model (Visual No-Code Builder + Advanced Expression DSL)**:
  - **Visual Builder Mode**: Guided definition of entity attributes, operators (`EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `LESS_THAN`, `IN`, `CONTAINS`), and action templates (`BLOCK_OPERATION`, `APPLY_DISCOUNT`, `SET_FIELD_VALUE`, `DISPATCH_NOTIFICATION`).
  - **Advanced DSL Mode**: Composes sandboxed boolean and assignment expressions (using MVEL/SpEL) for compound multi-attribute conditions and formulas.
- **FR-004**: System MUST allow filtering rules by tenant and operational scope (e.g., all companies, specific branch/filial, customer segment).
- **FR-005**: System MUST enforce strict security sandboxing preventing access to host OS, filesystem, network sockets, reflection, or unauthorized Spring beans.
- **FR-006**: System MUST enforce an execution timeout threshold (e.g., 200ms) with automated thread interruption.
- **FR-007**: System MUST provide priority ordering when multiple rules are attached to the same trigger event (lowest priority number executes first).
- **FR-008**: System MUST support configurable **Error Handling Policy** per rule:
  - `ABORT_TRANSACTION` (Strict mode): Aborts the domain operation immediately and rolls back the transaction if the rule encounters an evaluation error or unhandled exception.
  - `LOG_AND_CONTINUE` (Permissive mode): Records a warning/failure entry in the execution audit log and allows the business transaction to complete uninterrupted.
- **FR-009**: System MUST provide a dry-run test simulation endpoint (`POST /api/v1/rules/test`) allowing administrators to validate rule evaluation against sample JSON payloads without affecting production data.
- **FR-010**: System MUST provide an administrative UI view in the frontend allowing administrators to view, create, edit, toggle enable/disable, test, and inspect execution logs of custom rules.
- **FR-011**: All frontend views for the Rules Engine MUST adhere to the internationalization standard established in Feature 003, providing translations in `en-US` and `pt-BR`.

---

### Key Entities *(include if feature involves data)*

- **CustomRule**:
  - `id`: UUID.
  - `tenant_id`: UUID.
  - `name`: String (e.g., "Bloqueio de Desconto Excessivo").
  - `description`: String.
  - `trigger_event`: String (`sale.item_added`, `sale.finalized`, etc.).
  - `timing`: Enum (`BEFORE`, `AFTER`).
  - `authoring_mode`: Enum (`VISUAL_BUILDER`, `ADVANCED_DSL`).
  - `condition_expression`: Text (JSON structure for visual builder or expression string for DSL).
  - `action_type`: Enum (`BLOCK_OPERATION`, `APPLY_DISCOUNT`, `SET_FIELD_VALUE`, `DISPATCH_NOTIFICATION`, `CUSTOM_ACTION`).
  - `action_payload`: Text (Configured action parameters or message).
  - `priority`: Int (lower number = runs first, e.g. 10, 20, 30).
  - `is_active`: Boolean.
  - `error_handling_policy`: Enum (`ABORT_TRANSACTION`, `LOG_AND_CONTINUE`).
  - `created_at`: Instant.
  - `updated_at`: Instant.
- **RuleExecutionTrace**:
  - `id`: UUID.
  - `tenant_id`: UUID.
  - `rule_id`: UUID.
  - `trigger_event`: String.
  - `entity_type`: String.
  - `entity_id`: String.
  - `duration_ms`: Long.
  - `result`: Enum (`MATCHED`, `SKIPPED`, `ERROR`).
  - `error_detail`: Text (nullable).
  - `created_at`: Instant.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Business consultants can add new validation or calculation rules for specific company segments in under 5 minutes without writing Kotlin code, altering database schemas, or redeploying the backend.
- **SC-002**: Rule evaluation latency for synchronous `BEFORE` validation rules is under 10 milliseconds in 95% of executions.
- **SC-003**: 100% of malicious or runaway rules (infinite loops, memory hogs, filesystem access attempts) are safely interrupted and sandboxed without crashing the application.
- **SC-004**: Zero cross-tenant rule leakage: 100% of rules strictly execute only for the tenant that created them.
- **SC-005**: 100% of rule execution failures adhere to their configured error handling policy (`ABORT_TRANSACTION` vs `LOG_AND_CONTINUE`).

---

## Assumptions

- Spring Modulith event interceptor and `PreCommitInterceptor` infrastructure from Feature 001 will be utilized as the dispatching backbone for `BEFORE` and `AFTER` triggers.
- MVEL (MVFLEX Expression Language) or Spring Expression Language (SpEL) in a restricted, sandboxed `EvaluationContext` will serve as the core expression compilation engine.
- Visual builder conditions serialize cleanly to/from standard JSON condition trees (e.g. `{ "field": "unit_price", "operator": "LESS_THAN", "value": 10.00 }`).
- Rule management will be secured under a new IAM permission: `RULES_MANAGE`.
