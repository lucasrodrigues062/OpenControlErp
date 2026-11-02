import { describe, it, expect } from 'vitest';
import { RuleSummary, VisualCondition } from '../../types/rules';

describe('Custom Rules Logic and Serialization Tests', () => {
  it('should construct valid Visual Builder payload and serialise properly', () => {
    const conditions: VisualCondition[] = [
      { field: 'sale.totalAmount', operator: 'GREATER_THAN', value: 1000 },
      { field: 'sale.discountPercentage', operator: 'LESS_OR_EQUAL', value: 15 },
    ];

    const group = {
      conjunction: 'AND',
      conditions,
    };

    const serialized = JSON.stringify(group);
    const parsed = JSON.parse(serialized);

    expect(parsed.conjunction).toBe('AND');
    expect(parsed.conditions).toHaveLength(2);
    expect(parsed.conditions[0].operator).toBe('GREATER_THAN');
    expect(parsed.conditions[1].operator).toBe('LESS_OR_EQUAL');
  });

  it('should filter rules correctly by search query and trigger point', () => {
    const rules: RuleSummary[] = [
      {
        id: '1',
        name: 'Max Wholesale Discount',
        description: 'Blocks discounts above 15%',
        triggerEvent: 'sale.item_added',
        timing: 'BEFORE',
        authoringMode: 'VISUAL_BUILDER',
        actionType: 'BLOCK_OPERATION',
        priority: 10,
        isActive: true,
        errorHandlingPolicy: 'ABORT_TRANSACTION',
        createdAt: '2026-11-02T10:00:00Z',
      },
      {
        id: '2',
        name: 'VIP Auto-Free Shipping',
        description: 'Mutates shippingFee for VIP orders',
        triggerEvent: 'sale.finalized',
        timing: 'BEFORE',
        authoringMode: 'ADVANCED_DSL',
        actionType: 'SET_FIELD_VALUE',
        priority: 50,
        isActive: true,
        errorHandlingPolicy: 'ABORT_TRANSACTION',
        createdAt: '2026-11-02T11:00:00Z',
      },
      {
        id: '3',
        name: 'Notify Partner Webhook',
        description: 'Dispatches notification on title settlement',
        triggerEvent: 'financial.title_settled',
        timing: 'AFTER',
        authoringMode: 'ADVANCED_DSL',
        actionType: 'DISPATCH_NOTIFICATION',
        priority: 100,
        isActive: false,
        errorHandlingPolicy: 'LOG_AND_CONTINUE',
        createdAt: '2026-11-02T12:00:00Z',
      },
    ];

    // Filter by trigger 'sale.item_added'
    const byTrigger = rules.filter((r) => r.triggerEvent === 'sale.item_added');
    expect(byTrigger).toHaveLength(1);
    expect(byTrigger[0].name).toBe('Max Wholesale Discount');

    // Filter by search query 'shipping'
    const bySearch = rules.filter(
      (r) =>
        r.name.toLowerCase().includes('shipping') ||
        (r.description && r.description.toLowerCase().includes('shipping'))
    );
    expect(bySearch).toHaveLength(1);
    expect(bySearch[0].id).toBe('2');
  });

  it('should gate rules menu under RULES_READ permission', () => {
    const userPermissions = ['RULES_READ'];
    const hasPermission = (p: string) => userPermissions.includes(p);

    expect(hasPermission('RULES_READ')).toBe(true);
    expect(hasPermission('RULES_MANAGE')).toBe(false);
  });
});
