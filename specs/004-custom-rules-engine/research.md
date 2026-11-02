# Research: Dynamic Custom Business Rules Engine

**Feature**: `004-custom-rules-engine`  
**Date**: 2026-11-02  
**Status**: Completed  

---

## Technical Decisions & Trade-offs

### 1. Expression Engine Selection: Sandboxed SpEL vs External Script Engines

#### Decision: Spring Expression Language (SpEL) with `SimpleEvaluationContext`
We select **SpEL** with a hardened `SimpleEvaluationContext` as the core expression compilation and evaluation engine.

#### Rationale:
- **Zero Third-Party Dependency Risk**: SpEL is natively integrated into Spring Boot 4.x / Kotlin and requires no external jars with known deserialization or classloading vulnerabilities.
- **Built-in Secure Sandboxing**: `SimpleEvaluationContext.forReadOnlyDataBinding()` strictly prevents:
  - Access to arbitrary Java classes (`T(java.lang.Runtime)`).
  - Reflection and method invocations on unauthorized objects.
  - Access to system properties or environment variables.
  - ClassLoader leaks.
- **Microsecond Latency**: SpEL expressions are parsed once into an AST (`SpelExpression`) and cached in memory per rule. Evaluation executes in `< 2 milliseconds`, fulfilling the `< 5ms` requirement for transactional POS / order processing.
- **Native Support for Both Authoring Modes**:
  - In **Visual Builder Mode**, the condition tree (e.g. `[field: "discountPercentage", op: "GREATER_THAN", val: 20]`) converts deterministically into a SpEL expression: `#item.discountPercentage > 20`.
  - In **Advanced DSL Mode**, power users compose standard SpEL expressions directly with auto-completion and syntax checks.

#### Alternatives Considered:
- *MVEL (`org.mvel:mvel2`)*: Fast, but has documented sandboxing bypasses in multi-tenant environments when untrusted users author expressions.
- *Groovy / GraalVM JS*: Powerful, but excessive memory footprint, heavy initialization latency, and complex CPU/memory thread throttling.
- *Dynamic SQL (`SELECT ... WHERE ...`)*: Rejected due to SQL injection risks, database network latency on high-frequency POS triggers, and inability to inspect/mutate in-flight memory objects prior to commit.

---

### 2. Trigger Interception & Execution Lifecycle

#### Decision: Unified Pre-Commit & Post-Commit Interceptor Hooks
The engine hooks directly into the ERP's Domain Event Bus established in Feature 001:
1. **`BEFORE` (Synchronous Pre-Commit Interceptors)**:
   - Implements `PreCommitInterceptor` from `org.opencontrol.erp.kernel.event`.
   - Intercepts domain mutations in the current Hibernate/JPA transaction.
   - Evaluates active rules matching the trigger event ordered by `priority ASC`.
   - If an action is `BLOCK_OPERATION`, throws `BusinessRuleViolationException` with the customized user message, rolling back the database transaction.
   - If an action is `APPLY_DISCOUNT` or `SET_FIELD_VALUE`, mutates the target memory object before persisting.
2. **`AFTER` (Asynchronous Post-Commit Listeners)**:
   - Listens to committed domain events via `@TransactionalEventListener(phase = AFTER_COMMIT)`.
   - Executes asynchronously via Java 21 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`).
   - Dispatches side-effects (e.g., audit notification, external webhook) without blocking the user response.

---

### 3. Circuit Breaking, Timeouts, and Error Handling Policies

#### Decision: Guarded Execution with Configurable Policy (`ABORT` vs `LOG_AND_CONTINUE`)
1. **Timeout Guard**:
   - Each rule evaluation is governed by a `CompletableFuture` or timeout wrapper capped at 200ms.
2. **Policy Enforcement**:
   - `ABORT_TRANSACTION` (Strict): If evaluation fails with an unexpected exception (e.g., null pointer, division by zero), rolls back transaction with an explicit error.
   - `LOG_AND_CONTINUE` (Permissive): Records a failure trace in `rule_execution_trace` and allows the business transaction to complete uninterrupted.
3. **Recursion Limit**:
   - Execution context tracks `depth`. Max depth is capped at 1 to prevent cascading trigger loops.

---

### 4. Interactive Simulation & Dry-Run API

#### Decision: In-Memory Payload Simulation (`POST /api/v1/rules/test`)
Provides an endpoint where administrators supply a rule definition and a sample JSON payload. The engine parses the expression, evaluates against the mock context, and returns:
- `matched`: `boolean`
- `actionApplied`: action description
- `durationMs`: execution time in ms
- `diagnostics`: syntax or evaluation errors if any
Zero database state is touched during simulations.
