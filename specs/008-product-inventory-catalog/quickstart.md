# Quickstart Guide: Product Master Data, Brazilian Taxation, Multi-Warehouse Inventory & Traceability

**Feature Branch**: `008-product-inventory-catalog`  
**Date**: 2026-11-07  
**Spec Reference**: [specs/008-product-inventory-catalog/spec.md](specs/008-product-inventory-catalog/spec.md)

This guide provides end-to-end operational verification scenarios for the Product Catalog and Inventory module.

---

## Prerequisites

1. PostgreSQL database running with migration `V1_0_11__create_product_and_inventory_tables.sql` applied.
2. Spring Boot backend active on `http://localhost:8080`.
3. Valid authentication JWT token with `CATALOG_ADMIN` and `INVENTORY_ADMIN` permissions.

---

## Scenario 1: Product Master Data with Brazilian NCM, CEST & Barcode

### 1.1 Create Category
```bash
POST /api/v1/catalog/categories
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "code": "ELETRONICOS",
  "name": "Equipamentos Eletrônicos & Informática",
  "colorHex": "#3B82F6"
}
```
*Expected Result*: Status `201 Created` with category UUID.

### 1.2 Register Product
```bash
POST /api/v1/catalog/products
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "sku": "NBK-DELL-I7",
  "barcode": "7891234567890",
  "name": "Notebook Dell Latitude i7 16GB 512GB SSD",
  "itemType": "REV_00",
  "unitOfMeasure": "UN",
  "categoryId": "<CATEGORY_UUID>",
  "brand": "Dell",
  "ncm": "84713012",
  "cest": "2100100",
  "origin": 0,
  "trackingMode": "SERIAL_NUMBER",
  "minStock": 5,
  "maxStock": 50,
  "reorderPoint": 10,
  "standardSalesPrice": 5499.00,
  "acquisitionCost": 3800.00
}
```
*Expected Result*: Status `201 Created`. EAN-13 and NCM format verified.

---

## Scenario 2: Multi-Warehouse Setup & Logistical Bin Addressing

### 2.1 Create Main Distribution Warehouse
```bash
POST /api/v1/inventory/warehouses
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "code": "CD-CENTRAL",
  "name": "Centro de Distribuição Central",
  "branchId": "<BRANCH_UUID>",
  "warehouseType": "COMMERCIAL",
  "isDefault": true
}
```
*Expected Result*: Status `201 Created`.

### 2.2 Configure Storage Bin Address
```bash
POST /api/v1/inventory/warehouses/<WAREHOUSE_UUID>/locations
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "aisle": "A",
  "rack": "04",
  "shelf": "02",
  "bin": "01"
}
```
*Expected Result*: Status `201 Created`, formatted address generated as `A-04-02-01`.

---

## Scenario 3: Lot/Batch Inbound & FEFO (First Expire, First Out) Dispatch

### 3.1 Receive Lot-Controlled Product
Register a chemical/pharmaceutical item (`trackingMode: BATCH_LOT`) and post receipt of two distinct batches:
- Batch `LOT-A`: Manufacturing 2026-01-10, Expiration **2026-12-31** (Qty: 100)
- Batch `LOT-B`: Manufacturing 2026-03-01, Expiration **2027-06-30** (Qty: 200)

```bash
POST /api/v1/inventory/movements
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "productId": "<LOT_PRODUCT_UUID>",
  "movementType": "INBOUND_PURCHASE",
  "destinationWarehouseId": "<WAREHOUSE_UUID>",
  "lotNumber": "LOT-A",
  "manufacturingDate": "2026-01-10",
  "expirationDate": "2026-12-31",
  "quantity": 100,
  "unitCost": 45.00
}
```

### 3.2 Verify FEFO Allocation
Query active lots for the product:
```bash
GET /api/v1/inventory/lots?productId=<LOT_PRODUCT_UUID>&availableOnly=true
```
*Expected Result*: `LOT-A` appears first in the array because its expiration date is earlier, ensuring FEFO adherence.

---

## Scenario 4: Serialized Inventory Unit Tracking

### 4.1 Inbound Receipt with Serial Numbers
```bash
POST /api/v1/inventory/movements
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "productId": "<NBK_PRODUCT_UUID>",
  "movementType": "INBOUND_PURCHASE",
  "destinationWarehouseId": "<WAREHOUSE_UUID>",
  "serialNumbers": ["DELL-SN-001", "DELL-SN-002"],
  "quantity": 2,
  "unitCost": 3800.00
}
```
*Expected Result*: 2 serial numbers created in status `IN_STOCK`. Stock balance on hand becomes `2`.

---

## Scenario 5: Stock Transfer & Kardex Audit Trail

### 5.1 Execute Inter-Warehouse Transfer
```bash
POST /api/v1/inventory/movements
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "productId": "<NBK_PRODUCT_UUID>",
  "movementType": "TRANSFER_OUT",
  "sourceWarehouseId": "<CD_CENTRAL_UUID>",
  "destinationWarehouseId": "<LOJA_FILIAL_UUID>",
  "serialNumbers": ["DELL-SN-001"],
  "quantity": 1,
  "reasonCode": "REPLENISHMENT_STORE",
  "notes": "Transferência de mostruário para loja física"
}
```

### 5.2 Inspect Kardex Ledger
```bash
GET /api/v1/inventory/movements?productId=<NBK_PRODUCT_UUID>
```
*Expected Result*: Returns complete chronological movements showing exact decrement in CD-Central and increment in Loja Filial, with user ID, timestamps, and balance snapshots.
