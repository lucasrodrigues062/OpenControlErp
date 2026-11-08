# Quickstart & Verification Guide: Inbound Purchases, Fiscal Invoicing, Landed Costing & Inventory Transfers

**Feature**: `009-inbound-purchases-transfers`
**Date**: 2026-11-08

This guide describes end-to-end verification scenarios to test the full lifecycle of procurement, NF-e XML ingestion, dynamic Excel-like landed cost evaluation, physical goods receipt, and inter-branch transfers.

---

## Scenario 1: Configure Custom Excel-Like Landed Cost Formula Profile

1. **Create Cost Formula Profile**:
   - Send `POST /api/v1/inbound/cost-formula-profiles` with payload:
     ```json
     {
       "code": "LUCRO_REAL_IND",
       "name": "Indústria - Lucro Real com Crédito Total e Frete Rateado",
       "formulaExpression": "= INVOICE_UNIT_PRICE + ((FREIGHT_ALLOCATED + INSURANCE_ALLOCATED + OTHER_EXPENSES) / QUANTITY) + IPI_COST - (ICMS_CREDIT + PIS_CREDIT + COFINS_CREDIT)",
       "freightProrationMethod": "VALUE_PROPORTIONAL",
       "isDefault": true
     }
     ```
2. **Simulate Formula via Test Endpoint**:
   - Call `POST /api/v1/inbound/cost-formula-profiles/evaluate-test` with test variables:
     - `INVOICE_UNIT_PRICE`: 100.00
     - `QUANTITY`: 10
     - `FREIGHT_ALLOCATED`: 50.00 (i.e. 5.00/unit)
     - `INSURANCE_ALLOCATED`: 10.00 (i.e. 1.00/unit)
     - `OTHER_EXPENSES`: 0.00
     - `IPI_COST`: 10.00
     - `ICMS_CREDIT`: 12.00
     - `PIS_CREDIT`: 1.65
     - `COFINS_CREDIT`: 7.60
   - **Expected Outcome**:
     $$\text{Landed Cost} = 100 + \frac{50 + 10}{10} + 10 - (12 + 1.65 + 7.60) = 100 + 6 + 10 - 21.25 = 94.75$$
     System returns `calculatedLandedUnitCost: 94.7500`.

---

## Scenario 2: Purchase Order Creation & Approval

1. **Create Purchase Order**:
   - Send `POST /api/v1/inbound/purchase-orders` specifying supplier, delivery warehouse, and items.
   - Status defaults to `DRAFT`.
2. **Approve Purchase Order**:
   - Send `POST /api/v1/inbound/purchase-orders/{id}/approve`.
   - Status changes to `APPROVED`.

---

## Scenario 3: Upload Inbound NF-e XML & Landed Cost Execution

1. **Upload XML File**:
   - Upload sample NF-e XML via `POST /api/v1/inbound/invoices/upload-xml`.
   - System extracts 44-digit access key, invoice number, issuer CNPJ, and items.
2. **Item Correlation & De-Para**:
   - If vendor product is new, pair with catalog SKU and set conversion factor (e.g. 1 CX = 12 UN).
   - Cost engine applies the active formula and computes landed unit cost per line item.
3. **Commit Inventory (Finalize Receipt)**:
   - Send `POST /api/v1/inbound/invoices/{id}/finalize-stock`.
   - Double-entry stock movements (`stock_movements`) are written.
   - Product Weighted Average Cost (CMP) and last acquisition cost are updated.

---

## Scenario 4: Inter-Branch Stock Transfer with In-Transit State

1. **Create Inter-Branch Transfer**:
   - Send `POST /api/v1/inbound/transfers` with `transferType: "INTER_BRANCH"`, origin Branch A, destination Branch B, product, quantity, and lot.
2. **Dispatch Transfer**:
   - Send `POST /api/v1/inbound/transfers/{id}/dispatch`.
   - Branch A inventory decreases by 20 units.
   - Status transitions to `DISPATCHED_IN_TRANSIT`.
   - Verify that destination Branch B stock is NOT yet available for sale.
3. **Receive at Destination Branch**:
   - Send `POST /api/v1/inbound/transfers/{id}/receive`.
   - Branch B inventory increases by 20 units with identical lot and valuation.
   - Status transitions to `RECEIVED_COMPLETED`.
