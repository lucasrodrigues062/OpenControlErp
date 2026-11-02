# Implementation Plan: Custom Business Rules Engine

**Branch**: `004-custom-rules-engine` | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-custom-rules-engine/spec.md`

## Summary

Implement a multi-tenant **Custom Business Rules Engine** for the OpenControl ERP platform. The engine empowers diverse business segments (retail, wholesale, manufacturing, logistics, services) to define dynamic validation, field mutation, tagging, and notification rules without changing core application code.

Key architecture highlights:
1. **Hybrid Rule Authoring**: Visual No-Code Tree Builder for business operators and Advanced Expression DSL (sandboxed Spring Expression Language - SpEL) for power users.
2. **Dual-Timing Lifecycle Interception**:
   - `BEFORE` hooks: Synchronous interception using `PreCommitInterceptor` to validate and enforce business invariant checks (e.g. blocking transactions via `BusinessRuleViolationException`) or mutating fields prior to database flush.
   - `AFTER` hooks: Asynchronous/post-commit event listener running on Java 21 Virtual Threads for notifications, webhooks, and side-effects.
3. **Execution Safety & Configurable Failure Policies**:
   - Sandboxed SpEL using `SimpleEvaluationContext.forReadOnlyDataBinding()` to prevent JVM reflection, file system access, or arbitrary code execution.
   - Configurable failure policies per rule: `ABORT_TRANSACTION` (strict mode rolling back transactions) vs `LOG_AND_CONTINUE` (permissive mode logging error trace while preserving the business transaction).
   - Infinite recursion protection (depth limit $\le 5$) and timeout circuit breaker ($< 50\text{ms}$).
4. **Dry-Run Testing & Execution Tracing**:
   - Test endpoint (`POST /api/v1/rules/test`) executing rules against mock payloads with microsecond profiling.
   - Persistent execution trace log (`rule_execution_trace`) auditing rule execution, duration, status, and context snapshots.
5. **Modern Frontend Interface**:
   - React 18/19 SPA view (`/rules`) with visual condition editor, SpEL syntax validator, rule simulator drawer, trace explorer, and full `en-US` / `pt-BR` i18n support.

## Technical Context

**Language/Version**: Kotlin 2.2 / JVM 21 LTS (Backend), TypeScript 5.6+ / React 18/19 (Frontend)  
**Primary Dependencies**: Spring Boot 4.1.2, Spring Expression Language (SpEL), Spring Security, Flyway, React, Lucide React, Tailwind CSS, TanStack Query, Vitest  
**Storage**: PostgreSQL (`custom_rule`, `rule_execution_trace`), Flyway migration `V1_0_7`  
**Testing**: JUnit 5, MockK, SpringMockK, JaCoCo (>= 90% coverage gate), Vitest  
**Target Platform**: Modern Web Browsers & JVM 21 LTS on Linux/Windows containers  
**Project Type**: Full-stack Web Application (Modular Monolith Backend + Vite React SPA)  
**Performance Goals**: Individual rule evaluation $\le 2\text{ms}$, total rule chain per lifecycle trigger $\le 25\text{ms}$  
**Constraints**: Zero reflection/IO in SpEL evaluations; strict multi-tenant boundary checks on all rule queries and evaluations; 100% English code/docs; >= 90% JaCoCo coverage  
**Scale/Scope**: System-wide trigger points (`sale.*`, `financial.*`, `product.*`, `customer.*`)  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: Domain-Driven Modular Monolith**: Rules engine encapsulated inside `org.opencontrol.erp.rules` module with cleanly defined public service APIs and event listeners. (PASS)
- **Principle IV: Multi-Tenancy**: All database tables (`custom_rule`, `rule_execution_trace`) require `tenant_id NOT NULL`, with automatic indexing and tenant isolation in service specifications. (PASS)
- **Principle VII: Zero-Tolerance Documentation & Test Coverage**: Plan includes exhaustive unit, integration, and security tests with JaCoCo verification gate $\ge 90\%$. (PASS)
- **Code Documentation & Language**: English remains the primary code and technical doc language. User interface texts completely translated across `en-US` and `pt-BR`. (PASS)
- **Security & Sandboxing**: Strict SpEL sandboxing using `SimpleEvaluationContext` preventing ClassLoader/Runtime exploits. (PASS)

## Project Structure

### Documentation (this feature)

```text
specs/004-custom-rules-engine/
├── spec.md              # Feature specification
├── plan.md              # This implementation plan
├── research.md          # Technical research & engine benchmarks
├── data-model.md        # Database schema, entities & DTOs
├── quickstart.md        # Runnable validation scenarios
├── contracts/
│   └── custom-rules-api.yaml # OpenAPI 3.0 specification
└── checklists/
    └── requirements.md
```

### Source Code

```text
backend/
├── src/main/resources/db/migration/
│   └── V1_0_7__create_custom_rules_tables.sql
├── src/main/kotlin/org/opencontrol/erp/rules/
│   ├── api/
│   │   ├── RuleController.kt             # CRUD, toggle & simulation endpoints
│   │   └── dto/RuleDtos.kt               # RuleRequestDto, RuleResponseDto, SimulationDto
│   ├── domain/
│   │   ├── CustomRule.kt                 # JPA entity for custom rules
│   │   ├── RuleExecutionTrace.kt         # JPA entity for execution logs
│   │   └── RuleEnums.kt                  # TriggerPoint, Timing, RuleType, FailurePolicy, ActionType
│   ├── repository/
│   │   ├── CustomRuleRepository.kt       # Multi-tenant query repository
│   │   └── RuleExecutionTraceRepository.kt
│   └── service/
│       ├── RuleEvaluationEngine.kt       # Sandboxed SpEL evaluator & AST compiler
│       ├── RuleExecutionService.kt      # Orchestration, cycle detection & trace logging
│       └── RuleInterceptor.kt           # PreCommit & DomainEventListener hooks
└── src/test/kotlin/org/opencontrol/erp/rules/
    ├── api/RuleControllerTest.kt
    ├── service/RuleEvaluationEngineTest.kt
    ├── service/RuleExecutionServiceTest.kt
    └── service/RuleSecuritySandboxTest.kt

frontend/
├── src/
│   ├── api/rulesApi.ts                   # Rules CRUD & simulation API client
│   ├── components/rules/
│   │   ├── RuleFormModal.tsx             # Modal with Visual / DSL tabs
│   │   ├── VisualConditionBuilder.tsx    # No-code field-operator-value builder
│   │   ├── DslCodeEditor.tsx             # SpEL expression editor with syntax hints
│   │   ├── RuleSimulationDrawer.tsx      # Interactive dry-run sandbox
│   │   └── RuleTracesModal.tsx           # Execution audit viewer
│   ├── locales/
│   │   ├── en-US/rules.ts                # English i18n dictionary
│   │   └── pt-BR/rules.ts                # Brazilian Portuguese i18n dictionary
│   ├── pages/
│   │   └── RulesPage.tsx                 # Management table, filtering, activation toggle
│   └── types/
│       └── rules.ts                      # TypeScript models for rules, conditions, actions, traces
└── src/pages/__tests__/
    └── RulesPage.test.tsx                # Frontend unit & integration tests
```

## Structure Decision

The rules engine is implemented as a dedicated core module `org.opencontrol.erp.rules` in the modular monolith backend. It integrates seamlessly with domain entity lifecycles through Spring's interceptor and application event mechanisms without polluting domain business models. The frontend lives cleanly under `/rules` with full internationalization (`en-US` and `pt-BR`).
