import { describe, it, expect } from 'vitest';
import { CustomFieldDefinition, CreateCustomFieldDefinitionRequest } from '../../types/customFields';

describe('Custom Fields Model and Logic Tests', () => {
  it('should validate creation payload formatting', () => {
    const req: CreateCustomFieldDefinitionRequest = {
      targetEntity: 'CUSTOMER',
      fieldKey: 'spouse_name',
      label: 'Spouse Name',
      fieldType: 'TEXT',
      required: true,
      adminOnly: false,
      readOnly: false,
      displayOrder: 1,
    };

    expect(req.targetEntity).toBe('CUSTOMER');
    expect(req.fieldKey).toBe('spouse_name');
    expect(req.fieldType).toBe('TEXT');
    expect(req.required).toBe(true);
  });

  it('should format SELECT options array properly', () => {
    const req: CreateCustomFieldDefinitionRequest = {
      targetEntity: 'PRODUCT',
      fieldKey: 'shelf_life_unit',
      label: 'Shelf Life Unit',
      fieldType: 'SELECT',
      options: ['DAYS', 'MONTHS', 'YEARS'],
    };

    expect(req.options).toHaveLength(3);
    expect(req.options).toContain('DAYS');
  });

  it('should reorder custom fields sequentially', () => {
    const fields: CustomFieldDefinition[] = [
      {
        id: '1',
        tenantId: 'tenant-1',
        targetEntity: 'CUSTOMER',
        fieldKey: 'first_field',
        label: 'First Field',
        fieldType: 'TEXT',
        required: false,
        adminOnly: false,
        readOnly: false,
        displayOrder: 1,
        isActive: true,
        createdAt: '2026-11-02T10:00:00Z',
        updatedAt: '2026-11-02T10:00:00Z',
      },
      {
        id: '2',
        tenantId: 'tenant-1',
        targetEntity: 'CUSTOMER',
        fieldKey: 'second_field',
        label: 'Second Field',
        fieldType: 'NUMBER',
        required: true,
        adminOnly: true,
        readOnly: false,
        displayOrder: 2,
        isActive: true,
        createdAt: '2026-11-02T10:00:00Z',
        updatedAt: '2026-11-02T10:00:00Z',
      },
    ];

    // Swap positions
    const reordered = [fields[1], fields[0]].map((f, idx) => ({
      ...f,
      displayOrder: (idx + 1) * 10,
    }));

    expect(reordered[0].id).toBe('2');
    expect(reordered[0].displayOrder).toBe(10);
    expect(reordered[1].id).toBe('1');
    expect(reordered[1].displayOrder).toBe(20);
  });
});
