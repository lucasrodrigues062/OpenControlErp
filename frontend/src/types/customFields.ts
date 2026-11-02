export type FieldType = 'TEXT' | 'NUMBER' | 'DECIMAL' | 'DATE' | 'BOOLEAN' | 'SELECT';

export interface ValidationRules {
  min?: number;
  max?: number;
  regex?: string;
}

export interface TargetEntityDescriptor {
  entityCode: string;
  displayName: string;
  module: string;
  description?: string;
}

export interface CustomFieldDefinition {
  id: string;
  tenantId: string;
  targetEntity: string;
  fieldKey: string;
  label: string;
  description?: string;
  fieldType: FieldType;
  required: boolean;
  adminOnly: boolean;
  readOnly: boolean;
  defaultValue?: string;
  options?: string[];
  validationRules?: ValidationRules;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCustomFieldDefinitionRequest {
  targetEntity: string;
  fieldKey: string;
  label: string;
  description?: string;
  fieldType: FieldType;
  required?: boolean;
  adminOnly?: boolean;
  readOnly?: boolean;
  defaultValue?: string;
  options?: string[];
  validationRules?: ValidationRules;
  displayOrder?: number;
}

export interface UpdateCustomFieldDefinitionRequest {
  label: string;
  description?: string;
  required?: boolean;
  adminOnly?: boolean;
  readOnly?: boolean;
  defaultValue?: string;
  options?: string[];
  validationRules?: ValidationRules;
  displayOrder?: number;
}

export interface FieldOrderDto {
  id: string;
  displayOrder: number;
}

export interface EntityCustomFieldsResponse {
  targetEntity: string;
  entityId: string;
  values: Record<string, any>;
  updatedAt?: string;
}

export interface SaveEntityCustomFieldsRequest {
  values: Record<string, any>;
}
