# Feature Specification: Inbound Purchases, Fiscal Invoicing, Landed Costing & Inventory Transfers

**Feature Branch**: `009-inbound-purchases-transfers`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: "Agora, vamos para toda a parte de entrada, Pedidos ao fornecedor, entrada de nota fiscal, entrada de mercadoria, transferencia entre estoque, transferencia entre filiais, Calculo de preço de custo, tipos de custos, lançamento de frete, etc.."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Purchase Orders to Suppliers (Priority: P1)

Purchasing managers and buyers need to create and track purchase orders to approved suppliers, specifying requested items, agreed unit prices, delivery terms, payment conditions, and target warehouses/branches. The purchase order serves as the commercial commitment and the contractual baseline for matching incoming goods and fiscal invoices.

**Why this priority**: Purchase orders represent the starting point of the inbound supply chain. Without a purchase order, businesses cannot control procurement commitments, compare negotiated prices against invoiced amounts, or plan inbound receiving operations.

**Independent Test**: A purchasing agent creates a purchase order with multiple product lines, sets expected arrival dates and payment terms, moves it to "Approved", and views the purchase order status. The order displays the negotiated quantities and totals, ready for receiving.

**Acceptance Scenarios**:

1. **Given** an approved vendor in the system, **When** a buyer creates a purchase order specifying items, quantities, agreed unit purchase prices, destination branch, and delivery warehouse, **Then** the purchase order is saved in "Draft" status with total gross amounts calculated.
2. **Given** a purchase order in "Draft", **When** an authorized supervisor approves it, **Then** the order changes to "Approved" and locks against informal price tampering.
3. **Given** an "Approved" purchase order, **When** warehouse operators receive goods linked to this order, **Then** the order tracking updates the fulfilled quantity per item, progressing from "Partially Received" to "Fully Received".

---

### User Story 2 - Inbound Fiscal Invoice Entry (NF-e XML & Manual) & Partner/Item Matching (Priority: P1)

Fiscal and accounts payable clerks need to record incoming invoices (Notas Fiscais de Entrada / NF-e) by either uploading the vendor's SEFAZ XML or manually keying in invoice data. The system must automatically recognize the supplier by CNPJ/CPF, link invoice items to internal catalog products using barcode/supplier item mapping with packaging unit conversions (e.g., box of 12 converted to individual units), and validate fiscal tax rates (ICMS, IPI, PIS, COFINS, ST).

**Why this priority**: Inbound invoices govern legal ownership of acquired inventory, statutory tax credit eligibility, and accounts payable obligations. Automated XML ingestion eliminates manual data entry errors and speeds up goods receiving.

**Independent Test**: Upload a standard SEFAZ NF-e XML file. The system parses invoice metadata, identifies or prompts matching for the supplier, matches line items to internal catalog SKUs via saved relationship rules, and displays the complete fiscal tax breakdown and CFOP classification.

**Acceptance Scenarios**:

1. **Given** a valid vendor NF-e XML file, **When** the clerk uploads it, **Then** the system extracts access key, invoice number, series, issuance date, supplier details, item details, freight, insurance, and tax values.
2. **Given** an uploaded invoice with unknown supplier item codes, **When** the user associates the supplier's code with an internal product SKU for the first time, **Then** the system remembers this correlation for all future imports.
3. **Given** an invoice item billed in a purchase unit different from internal storage (e.g., supplier billed in boxes "CX" with 24 units, internal stock is "UN"), **When** matched, **Then** the system applies the conversion factor to establish correct internal quantities and unit costs.

---

### User Story 3 - Physical Goods Receipt, Blind Conference & Lot/Serial Capture (Priority: P2)

Warehouse receiving teams need to perform physical inspection and count (blind or assisted conference) of arriving shipments against the purchase order or inbound invoice. During receiving, operators record lot numbers, manufacturing dates, expiration dates, or individual serial numbers for tracked items, flagging any quantitative or qualitative discrepancies before authorizing warehouse entry.

**Why this priority**: Ensures that physical inventory exactly matches statutory documentation, prevents warehouse contamination or missing shipments, and enforces traceability compliance (FEFO and warranty tracking).

**Independent Test**: Receive a shipment containing lot-controlled products and serial-tracked items, enter the verified counts, lot identifiers, and expiration dates, and verify that inventory is credited only to the designated quarantine or active warehouse location.

**Acceptance Scenarios**:

1. **Given** an arriving shipment tied to a purchase order or invoice, **When** warehouse staff perform the receiving check, **Then** the system records the verified physical quantities and highlights any differences (divergence between invoiced and delivered).
2. **Given** an item configured with batch/lot tracking, **When** received at the dock, **Then** the operator is required to input the lot number, manufacturing date, and expiration date before the goods can be posted.
3. **Given** an item configured with serial number tracking, **When** received, **Then** the operator enters or scans each individual serial number, preventing duplicate serial registrations.

---

### User Story 4 - Landed Cost Calculation & Inventory Valuation (CMP / Freight / Taxes) (Priority: P2)

Cost accountants and inventory managers need the system to calculate the true Landed Cost (Custo de Aquisição Efetivo) for every received item. The calculation must combine the purchase price with allocated inbound freight (frete por valor/peso), insurance, accessory handling expenses, customs fees, and non-recoverable taxes, while deducting statutory tax credits (ICMS, PIS, COFINS according to the branch tax regime). The system must automatically update the Weighted Average Cost (Custo Médio Ponderado - CMP) in the immutable inventory ledger according to statutory accounting guidelines (RIR / Decreto 9.580/2018).

**Why this priority**: Accurate cost accounting is essential for statutory financial statements, margin calculation, and sales pricing. Incorrect cost allocation distorts gross margins and can lead to financial losses or tax compliance penalties.

**Independent Test**: Register an inbound invoice for two products with distinct quantities and values, add a separate freight charge (conhecimento de frete) and handling fees, and verify that landed unit costs and new CMP are prorated and updated in the inventory ledger.

**Acceptance Scenarios**:

1. **Given** an inbound invoice with freight, insurance, and extra expenses, **When** finalizing goods entry, **Then** the system prorates additional landed costs across items proportionally to item value (or weight) and calculates the definitive landed unit cost.
2. **Given** a company branch operating under "Lucro Real" tax regime, **When** receiving merchandise for resale, **Then** recoverable ICMS, PIS, and COFINS taxes are deducted from the item's inventory cost.
3. **Given** a company branch operating under "Simples Nacional", **When** receiving merchandise, **Then** taxes remain absorbed into the acquisition cost without credit deductions.
4. **Given** an item with existing stock and CMP, **When** a new shipment arrives with a different unit cost, **Then** the system recalculates the statutory Weighted Average Cost: `(Current Qty * Current CMP + New Inbound Qty * New Landed Unit Cost) / Total Qty`.

---

### User Story 5 - Internal Warehouse Transfers & Inter-Branch Transfers (Priority: P3)

Logistics operators need to transfer stock between warehouses within the same branch (e.g., from Central Warehouse to Local Store / Production Line) and perform inter-branch transfers (e.g., Matriz to Filial) across different geographical locations. Inter-branch transfers must support in-transit stock tracking (dispatched from origin branch, moving, received at destination branch) and preserve product lots and serial numbers.

**Why this priority**: Multi-branch enterprises frequently balance inventory levels across distribution centers and retail stores. Without in-transit tracking and inter-branch visibility, stock vanishes from system ledgers during transportation.

**Independent Test**: Dispatch a transfer of 50 units (including a specific lot) from Branch A to Branch B. Verify that Branch A's available balance decreases, inventory enters "In-Transit", and once Branch B confirms receipt, Branch B's warehouse balance increases with the exact same lot and valuation.

**Acceptance Scenarios**:

1. **Given** sufficient stock in a source warehouse, **When** an operator executes an internal warehouse transfer within the same branch, **Then** the source warehouse balance decreases and destination warehouse balance increases immediately.
2. **Given** an inter-branch transfer order, **When** origin branch dispatches goods, **Then** the origin branch stock is reduced and transferred into a virtual "In-Transit" state.
3. **Given** a shipment in "In-Transit", **When** destination branch receives and inspects the goods, **Then** the destination stock is credited, "In-Transit" is cleared, and the transfer status transitions to "Completed".
4. **Given** goods transferred across branches with tracking (lots or serials), **When** received at destination, **Then** the exact lot identification, expiration dates, and serial numbers are preserved in the destination branch's ledger.

---

## Edge Cases

- **Partial Receiving Discrepancy**: Vendor sends fewer items than ordered or invoiced. The system allows registering partial receipt, marks the purchase order as "Partially Received", and provides an option to await remainder or close with a divergence report.
- **Excess Delivery**: Vendor delivers more than ordered on the purchase order. System flags the excess, requiring manager approval before admitting extra stock into inventory.
- **Zero or Negative Stock on Inbound Returns**: If goods need to be rejected or returned to supplier before stocking, the receipt is cancelled without corrupting warehouse balances.
- **Independent Freight Bill (CT-e) Arriving Later**: Freight is billed by a third-party carrier days after the product invoice was received and stocked. System allows linking an apportioned freight expense retroactively to adjust cost or post to inventory adjustment accounts.
- **Transfer Damage / Loss In-Transit**: During inter-branch transport, items are damaged or lost. The destination receiving check allows partial acceptance with a recorded loss entry, crediting destination stock only for undamaged units and posting discrepancies to an in-transit loss ledger.
- **Unit of Measure Decimal Precision**: Complex conversion factors (e.g., 1 ton to kilograms, or boxes with fractional units) maintain up to 4 decimal places without cumulative rounding truncation errors.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide creation, editing, cancellation, and approval workflows for Purchase Orders directed to registered suppliers.
- **FR-002**: Purchase Orders MUST support line items with product reference, quantity, negotiated unit cost, target delivery branch, target warehouse, and delivery deadlines.
- **FR-003**: System MUST support importing SEFAZ NF-e XML files (Layout 4.00), extracting header, issuer, recipient, products, taxes, freight, and volume tags.
- **FR-004**: System MUST allow manual entry of inbound invoices for scenarios where XML is unavailable.
- **FR-005**: System MUST maintain an automated matching repository between supplier product codes / EAN barcodes and internal catalog SKUs.
- **FR-006**: System MUST handle unit of measure conversions during inbound processing (e.g., vendor billing unit `CX` -> internal inventory unit `UN`) using defined packaging conversion factors.
- **FR-007**: System MUST support physical receiving conferences with discrepancy detection between invoiced/ordered quantities and physically delivered quantities.
- **FR-008**: System MUST require lot numbers, manufacturing dates, and expiration dates during inbound receipt for products marked with batch tracking mode (`BATCH_LOT`).
- **FR-009**: System MUST capture unique individual serial numbers during inbound receipt for products marked with serial tracking mode (`SERIAL_NUMBER`), preventing duplicate serial active states.
- **FR-010**: System MUST calculate Landed Cost per item by prorating purchase price, inbound freight, insurance, accessory expenses, and other cost types.
- **FR-011**: System MUST support multiple Landed Cost types (e.g., Inbound Freight CIF/FOB, Transit Insurance, Unloading/Handling, Customs Duty, Special Packaging).
- **FR-011b**: System MUST allow users to define Custom Variables within Cost Formula Profiles (e.g., custom rate coefficients, fixed handling fees, port surcharges, shrinkage percentages), which can be directly referenced in formula expressions and overridden per invoice if desired.
- **FR-012**: System MUST adjust item acquisition cost based on the branch's fiscal tax regime (deducting recoverable ICMS, PIS, COFINS for Lucro Real/Presumido, or fully absorbing taxes for Simples Nacional).
- **FR-013**: System MUST automatically update the item's statutory Weighted Average Cost (Custo Médio Ponderado - CMP) and Last Acquisition Cost (`last_acquisition_cost`) upon receiving completion.
- **FR-014**: System MUST write immutable double-entry ledger transactions (`stock_movements`) for all inbound receipts, detailing movement type, branch, warehouse, product, quantity, and unit acquisition cost.
- **FR-015**: System MUST support internal warehouse-to-warehouse stock transfers within the same company branch.
- **FR-016**: System MUST support inter-branch stock transfers between distinct company branches with multi-stage lifecycle (Draft, Dispatched / In-Transit, Received, Completed).
- **FR-017**: Inter-branch transfers MUST track "In-Transit" quantities in real-time, preventing items from being available for sale or double-counted while on the road.
- **FR-018**: Inter-branch transfers MUST maintain complete lot and serial number fidelity from origin branch to destination branch.
- **FR-019**: System MUST support retroactive or independent freight registration (e.g., CT-e transport bills) linked to existing inbound receipts or purchase orders.
- **FR-020**: System MUST enforce authorization permissions for purchase order approval, fiscal invoice entry, receiving conference, and inter-branch transfers.

---

### Key Entities

- **Purchase Order (`purchase_orders`)**: Header representing formal procurement contract with a vendor. Attributes: order number, tenant ID, branch ID, supplier ID, order date, expected delivery date, status (Draft, Approved, Partially Received, Completed, Cancelled), total order amount, payment terms, currency.
- **Purchase Order Item (`purchase_order_items`)**: Detail row for a purchase order. Attributes: product ID, quantity ordered, quantity received, unit price, discount amount, expected delivery warehouse.
- **Inbound Invoice (`inbound_invoices`)**: Fiscal invoice document received from vendor. Attributes: access key (Chave de Acesso NF-e - 44 digits), invoice number, series, issuance date, entry date, supplier ID, branch ID, total products, total freight, total insurance, total discount, total invoice amount, fiscal regime, status.
- **Inbound Invoice Item (`inbound_invoice_items`)**: Line item on the inbound invoice. Attributes: supplier item code, matched internal product ID, vendor unit of measure, conversion factor, internal quantity, unit purchase price, freight prorated, insurance prorated, expense prorated, ICMS tax, IPI tax, PIS tax, COFINS tax, calculated net landed unit cost.
- **Landed Cost Breakdown (`landed_cost_allocations`)**: Record of supplementary costs (freight, insurance, handling, customs) allocated across inbound invoice items.
- **Goods Receipt Note / Conference (`goods_receipts`)**: Physical receiving event document. Attributes: receipt date, branch ID, warehouse ID, purchase order reference, invoice reference, receiving operator, conference status (In Progress, Verified, Divergence Flagged, Stored).
- **Stock Transfer Order (`stock_transfers`)**: Header representing goods transfer. Attributes: transfer number, origin branch ID, destination branch ID, origin warehouse ID, destination warehouse ID, status (Draft, Dispatched / In-Transit, Received, Cancelled), dispatch date, receipt date, shipping tracking notes.
- **Stock Transfer Item (`stock_transfer_items`)**: Specific item line in transfer order. Attributes: product ID, requested quantity, shipped quantity, received quantity, lot ID, serial numbers list, transferred unit valuation.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can ingest a standard 50-item SEFAZ NF-e XML and match products within under 60 seconds when items have prior correlations.
- **SC-002**: Landed cost and Weighted Average Cost (CMP) calculations match statutory formulas with 100% mathematical precision (zero discrepancy in double-entry inventory ledger).
- **SC-003**: 100% of received lot-controlled and serial-tracked merchandise accurately retains traceability identifiers throughout subsequent warehouse operations and branch transfers.
- **SC-004**: Inter-branch inventory visibility updates in real-time, completely eliminating discrepancy in stock counts between dispatch and destination delivery.
- **SC-005**: Inbound discrepancy detection highlights 100% of quantity or price variances between purchase orders and received fiscal invoices.
- **SC-006**: Warehouse receiving conference can be executed by warehouse staff in under 3 minutes per standard purchase order delivery.

---

## Assumptions

- Supplier Master Data: Suppliers are registered or synced from the CRM/Partner directory (`crm_customers` / contacts) with valid CNPJ/CPF identifiers.
- Brazilian Fiscal Formats: Inbound XML files follow Brazilian National Tax Authority (SEFAZ) technical manual for NF-e (Layout 4.00, NF-e model 55).
- Tax Regime Configuration: Each company branch has its statutory tax regime defined (Simples Nacional vs Lucro Presumido vs Lucro Real) to dictate automatic tax credit eligibility.
- Cost Allocation Method: Default freight and accessory cost allocation method is based on item monetary value; weight-based proration is available when item physical weights are known.
- Double-Entry Inventory Principle: All stock movements continue to write immutable debit/credit entries to `stock_movements` table following established Kardex standards.
