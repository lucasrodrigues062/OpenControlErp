export type WarehouseType = 'COMMERCIAL' | 'STORAGE' | 'QUARANTINE' | 'TRANSIT';

export interface Warehouse {
  id: string;
  branchId: string;
  code: string;
  name: string;
  warehouseType: WarehouseType;
  isDefault: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWarehouseRequest {
  branchId: string;
  code: string;
  name: string;
  warehouseType?: WarehouseType;
  isDefault?: boolean;
}

export interface UpdateWarehouseRequest {
  branchId: string;
  name: string;
  warehouseType?: WarehouseType;
  isDefault?: boolean;
  isActive?: boolean;
}

export interface StorageLocation {
  id: string;
  warehouseId: string;
  aisle: string;
  rack: string;
  shelf: string;
  bin: string;
  formattedAddress: string;
  isActive: boolean;
  createdAt: string;
}

export interface CreateStorageLocationRequest {
  aisle: string;
  rack: string;
  shelf: string;
  bin: string;
}

export type LotStatus = 'RELEASED' | 'QUARANTINE' | 'BLOCKED' | 'EXPIRED';

export interface ProductLot {
  id: string;
  warehouseId: string;
  productId: string;
  lotNumber: string;
  manufacturingDate: string;
  expirationDate: string;
  quantityOnHand: number;
  quantityReserved: number;
  status: LotStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLotRequest {
  warehouseId: string;
  productId: string;
  lotNumber: string;
  manufacturingDate: string;
  expirationDate: string;
  quantityOnHand: number;
}

export type SerialNumberStatus =
  | 'IN_STOCK'
  | 'RESERVED'
  | 'DISPATCHED_SOLD'
  | 'RMA_MAINTENANCE'
  | 'WRITTEN_OFF';

export interface ProductSerialNumber {
  id: string;
  warehouseId: string;
  productId: string;
  lotId?: string | null;
  serialNumber: string;
  status: SerialNumberStatus;
  warrantyUntil?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSerialNumberRequest {
  warehouseId: string;
  productId: string;
  lotId?: string | null;
  serialNumber: string;
  warrantyUntil?: string | null;
}

export interface StockBalance {
  id: string;
  warehouseId: string;
  warehouseName?: string;
  productId: string;
  productSku?: string;
  productName?: string;
  locationId?: string | null;
  locationAddress?: string | null;
  quantityOnHand: number;
  quantityReserved: number;
  quantityAvailable: number;
  updatedAt: string;
}

export type StockMovementType =
  | 'INBOUND_PURCHASE'
  | 'OUTBOUND_SALE'
  | 'TRANSFER_OUT'
  | 'TRANSFER_IN'
  | 'ADJUSTMENT_INVENTORY'
  | 'RETURN_CUSTOMER'
  | 'RETURN_SUPPLIER';

export interface StockMovement {
  id: string;
  branchId: string;
  movementType: StockMovementType;
  productId: string;
  productSku?: string;
  productName?: string;
  sourceWarehouseId?: string | null;
  sourceWarehouseName?: string;
  destinationWarehouseId?: string | null;
  destinationWarehouseName?: string;
  locationId?: string | null;
  lotId?: string | null;
  serialNumberId?: string | null;
  quantity: number;
  unitCost: number;
  balanceAfter: number;
  referenceDocument?: string | null;
  reasonCode?: string | null;
  notes?: string | null;
  createdByUserId?: string | null;
  createdAt: string;
}

export interface CreateStockMovementRequest {
  branchId: string;
  movementType: StockMovementType;
  productId: string;
  sourceWarehouseId?: string | null;
  destinationWarehouseId?: string | null;
  locationId?: string | null;
  lotId?: string | null;
  serialNumberId?: string | null;
  quantity: number;
  unitCost?: number;
  referenceDocument?: string | null;
  reasonCode?: string | null;
  notes?: string | null;
}

export interface StockMovementPageResponse {
  items: StockMovement[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
