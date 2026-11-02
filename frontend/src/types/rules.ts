export type TriggerPoint =
  | 'sale.created'
  | 'sale.item_added'
  | 'sale.item_removed'
  | 'sale.finalized'
  | 'sale.cancelled'
  | 'product.created'
  | 'product.updated'
  | 'stock.movement_recorded'
  | 'financial.title_created'
  | 'financial.title_settled'
  | 'financial.title_cancelled';

export type RuleTiming = 'BEFORE' | 'AFTER';

export type AuthoringMode = 'VISUAL_BUILDER' | 'ADVANCED_DSL';

export type RuleActionType =
  | 'BLOCK_OPERATION'
  | 'APPLY_DISCOUNT'
  | 'SET_FIELD_VALUE'
  | 'DISPATCH_NOTIFICATION'
  | 'CUSTOM_ACTION';

export type ErrorHandlingPolicy = 'ABORT_TRANSACTION' | 'LOG_AND_CONTINUE';

export type TraceResult = 'MATCHED' | 'SKIPPED' | 'ERROR';

export type ComparisonOperator =
  | 'EQUALS'
  | 'NOT_EQUALS'
  | 'GREATER_THAN'
  | 'LESS_THAN'
  | 'GREATER_OR_EQUAL'
  | 'LESS_OR_EQUAL'
  | 'CONTAINS'
  | 'IN';

export interface VisualCondition {
  field: string;
  operator: ComparisonOperator;
  value: string | number | boolean;
}

export interface VisualConditionGroup {
  conjunction: 'AND' | 'OR';
  conditions: VisualCondition[];
}

export interface CustomRule {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  triggerEvent: TriggerPoint;
  timing: RuleTiming;
  authoringMode: AuthoringMode;
  conditionExpression: string;
  actionType: RuleActionType;
  actionPayload?: string;
  priority: number;
  isActive: boolean;
  errorHandlingPolicy: ErrorHandlingPolicy;
  createdAt: string;
  updatedAt: string;
}

export interface RuleSummary {
  id: string;
  name: string;
  description?: string;
  triggerEvent: TriggerPoint;
  timing: RuleTiming;
  authoringMode: AuthoringMode;
  actionType: RuleActionType;
  priority: number;
  isActive: boolean;
  errorHandlingPolicy: ErrorHandlingPolicy;
  createdAt: string;
}

export interface RuleRequest {
  name: string;
  description?: string;
  triggerEvent: TriggerPoint;
  timing: RuleTiming;
  authoringMode: AuthoringMode;
  conditionExpression: string;
  actionType: RuleActionType;
  actionPayload?: string;
  priority: number;
  isActive: boolean;
  errorHandlingPolicy: ErrorHandlingPolicy;
}

export interface RuleSimulationRequest {
  rule?: RuleRequest;
  ruleId?: string;
  context: Record<string, unknown>;
}

export interface RuleSimulationResponse {
  matched: boolean;
  actionType?: RuleActionType;
  actionPayload?: string;
  simulatedError?: string;
  mutatedContext?: Record<string, unknown>;
  durationMs: number;
  errorDetail?: string;
}

export interface RuleExecutionTrace {
  id: string;
  tenantId: string;
  ruleId: string;
  ruleName?: string;
  triggerEvent: string;
  entityType: string;
  entityId: string;
  durationMs: number;
  result: TraceResult;
  errorDetail?: string;
  createdAt: string;
}
