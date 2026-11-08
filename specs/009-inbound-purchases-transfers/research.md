# Research & Architectural Decisions: Inbound Purchases, Fiscal Invoicing, Landed Costing & Inventory Transfers

**Feature**: `009-inbound-purchases-transfers`
**Date**: 2026-11-08

---

## 1. Excel-Like Dynamic Cost Formula Engine (Motor de Cálculo de Custos Estilo Excel)

### Context & User Requirement
The user explicitly requested:
> *"Bom deixar esse motor de calculo de custo de preço bem extensivel pois cada tipo de empresa calcula seus custos diferentes, Se puder fazer um motor igual ao excel seia otimo"*

Enterprises across different tax regimes (Simples Nacional, Lucro Presumido, Lucro Real) and industries (wholesale distribution, retail, light manufacturing, food service, importation) compute acquisition costs with radically different statutory adjustments, extra freight apportionment methods, and custom internal expense allocations.

### Decision
Implement a **Configurable Landed Cost Formula Engine (`CostFormulaEngine`)** supporting Excel-like syntax, named variable injection, arithmetic operations, and conditional logic.

- **Formula Syntax**:
  Standard mathematical expressions with Excel-style operators and uppercase identifiers:
  ```excel
  = INVOICE_UNIT_PRICE + (FREIGHT_ALLOCATED + INSURANCE_ALLOCATED + OTHER_EXPENSES) / QUANTITY - (ICMS_CREDIT + PIS_CREDIT + COFINS_CREDIT)
  ```
- **Built-in Functions**:
  - `ROUND(value, decimals)` (e.g. `ROUND(x, 4)`)
  - `IF(condition, true_val, false_val)`
  - `MAX(a, b)`, `MIN(a, b)`
  - `COALESCE(val, fallback)`
- **Available Context Variables**:
  | Variable | Description |
  |---|---|
  | `INVOICE_UNIT_PRICE` | Unit price directly from invoice item |
  | `QUANTITY` | Inbound converted quantity in internal stock unit |
  | `FREIGHT_ALLOCATED` | Prorated inbound freight assigned to this item |
  | `INSURANCE_ALLOCATED` | Prorated transit insurance assigned to this item |
  | `OTHER_EXPENSES` | Prorated handling, unloading, accessory expenses |
  | `IPI_COST` | IPI tax per unit (when absorbed into inventory cost) |
  | `ST_COST` | Substituição Tributária (ICMS-ST) per unit |
  | `DIFAL_COST` | Diferencial de Alíquota tax per unit |
  | `CUSTOMS_DUTY` | Importation duties / II / Taxas aduaneiras |
  | `ICMS_CREDIT` | Statutory recoverable ICMS tax credit per unit |
  | `PIS_CREDIT` | Statutory recoverable PIS tax credit per unit |
  | `COFINS_CREDIT` | Statutory recoverable COFINS tax credit per unit |
  | `EXTRA_DISCOUNT` | Negotiated commercial deduction or rebate |
  | `[USER_CUSTOM_VARIABLE]` | Any user-defined variable declared in the profile (e.g. `TAXA_DESCARGA`, `MARGEM_PERDA`, `TAXA_DOLAR_EXTRA`) |

- **User-Defined Custom Variables (Variáveis Personalizadas pelo Usuário)**:
  - Users can declare custom named variables in their Cost Formula Profiles.
  - Each custom variable specifies:
    * `name`: Uppercase identifier (e.g., `TAXA_DESCARGA`, `FATOR_QUEBRA_PCT`, `TAXA_ARMAZENAGEM`).
    * `label`: Friendly display label (e.g., 'Taxa de Descarga por Lote', 'Margem de Quebra (%)').
    * `defaultValue`: Numeric default value (e.g., `15.00` or `0.02`).
    * `scope`: `PROFILE_DEFAULT` (fixed value across all invoices) or `PER_INVOICE_PROMPT` (can be informed when receiving an invoice).
  - The evaluator merges native variables + user custom variables into the evaluation scope seamlessly. Users can combine them in the formula:
    `= (INVOICE_UNIT_PRICE * (1 + FATOR_QUEBRA_PCT)) + (FREIGHT_ALLOCATED + TAXA_DESCARGA) / QUANTITY`
- **Evaluation Technology**:
  A secure Kotlin-native expression evaluator built on Spring Expression Language (SpEL) with a sandboxed context (no Java reflection, no class instantiation, strictly mathematical/logical functions) or tokenized AST.
- **Alternatives Considered**:
  - *Hardcoded formulas*: Inflexible, requires code changes for each company's tax nuances. Rejected.
  - *Full JavaScript / Nashorn / GraalJS script*: Heavyweight, security attack surface. Rejected.
  - *Rule Engine (Drools)*: Great for boolean classifications, but overly verbose and non-intuitive for accountants who think in Excel formulas. Rejected for user formula authoring.

---

## 2. SEFAZ NF-e XML Inbound Ingestion (Layout 4.00)

### Decision
Implement an `NFeXmlParserService` leveraging `Jackson-dataformat-xml` and standard DOM parsing:
- **Extraction**:
  - Header: Access key (Chave de Acesso - 44 digits), Number, Series, Issuance Date (`dhEmi`), Operation Type (0=Inbound, 1=Outbound).
  - Issuer (`emit`): CNPJ/CPF, Legal Name (`xNome`), Trade Name (`xFant`), State Registration (`IE`), Address.
  - Recipient (`dest`): CNPJ/CPF matching destination company branch.
  - Line Items (`det`): Item number (`nItem`), Product code (`cProd`), EAN (`cEAN`), Description (`xProd`), NCM, CEST, CFOP, Unit (`uCom`), Quantity (`qCom`), Unit Value (`vUnCom`), Total Item Value (`vProd`), Freight (`vFrete`), Insurance (`vSeg`), Discount (`vDesc`), Outras Despesas (`vOutro`).
  - Taxes: ICMS (CST, vBC, pICMS, vICMS, vICMSST), IPI (vIPI), PIS (vPIS), COFINS (vCOFINS).
  - Volumes & Carrier: Freight mode (0=CIF, 1=FOB), Carrier CNPJ, Weight.
- **Supplier & Product Correlation**:
  - Automatically lookup partner by CNPJ in `crm_customers` (with supplier role). If not found, offer 1-click vendor registration from XML data.
  - Automatically lookup product mapping in `supplier_product_mappings` (`tenant_id`, `supplier_id`, `supplier_product_code`). If mapped, auto-populate catalog SKU and packaging unit conversion factor (`conversion_factor`).

---

## 3. Physical Conference, Divergence Management & Traceability

### Decision
- Receiving process has a dedicated lifecycle:
  1. `DRAFT`: XML uploaded or PO linked.
  2. `IN_CONFERENCE`: Warehouse operator counting at dock.
  3. `CONFERENCE_VERIFIED`: All items counted; system highlights divergences:
     - *Quantity Divergence*: Delivers $\neq$ Invoiced / Ordered.
     - *Price Divergence*: Invoiced unit price exceeds Approved Purchase Order limit.
  4. `COMPLETED`: Stock balances credited, lots/serials created, CMP recalculated.
- **Traceability Enforcement**:
  - Products with `tracking_mode == 'BATCH_LOT'` require `lot_number`, `fabrication_date`, and `expiration_date` in conference.
  - Products with `tracking_mode == 'SERIAL_NUMBER'` require entering/scanning exact quantity of unique serial numbers.

---

## 4. Statutory Weighted Average Cost (Custo Médio Ponderado - CMP) & Inventory Valuation

### Decision
Follow Decreto Federal 9.580/2018 (Regulamento do Imposto de Renda - RIR), Art. 295:
$$\text{Novo CMP} = \frac{(\text{Estoque Atual} \times \text{CMP Anterior}) + (\text{Qtd Entrada} \times \text{Custo Unitário Efetivo})}{\text{Estoque Atual} + \text{Qtd Entrada}}$$
- If current stock $\le 0$ (e.g. initial stock), $\text{Novo CMP} = \text{Custo Unitário Efetivo}$.
- All stock movements write an immutable record to `stock_movements` with `movement_type = 'INBOUND_PURCHASE'`.

---

## 5. Inter-Branch Transfers & In-Transit Stock

### Decision
Inter-branch transfers (Filial A $\rightarrow$ Filial B) require a 2-step physical and accounting handshake:
1. **Dispatch (Saída por Transferência)**:
   - Branch A issues dispatch with CFOP 5.408/5.409/6.408.
   - Branch A stock is deducted: `movement_type = 'TRANSFER_OUT'`.
   - Inventory is held in a virtual transit bucket: `status = 'IN_TRANSIT'`.
2. **Reception (Entrada por Transferência)**:
   - Branch B inspects and confirms receipt: `movement_type = 'TRANSFER_IN'`.
   - Branch B stock is credited at the transferred cost valuation.
   - Traceability (Lots, Expiration, Serial Numbers) is cloned and linked to Branch B.
