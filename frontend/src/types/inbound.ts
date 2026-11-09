export type PurchaseOrderStatus = 'DRAFT' | 'APPROVED' | 'PARTIALLY_RECEIVED' | 'FULLY_RECEIVED' | 'CANCELLED';

export interface PurchaseOrderItem {
  id?: string;
  productId: string;
  productName?: string;
  productSku?: string;
  targetWarehouseId?: string;
  quantityOrdered: number;
  quantityReceived?: number;
  unitPrice: number;
  discountAmount?: number;
  totalPrice?: number;
}

export interface PurchaseOrder {
  id: string;
  orderNumber: string;
  branchId: string;
  supplierId: string;
  supplierName?: string;
  orderDate: string;
  expectedDeliveryDate?: string | null;
  status: PurchaseOrderStatus;
  totalAmount: number;
  currency: string;
  paymentTerms?: string | null;
  notes?: string | null;
  items: PurchaseOrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface CreatePurchaseOrderRequest {
  orderNumber?: string;
  branchId: string;
  supplierId: string;
  orderDate: string;
  expectedDeliveryDate?: string | null;
  paymentTerms?: string | null;
  notes?: string | null;
  items: {
    productId: string;
    targetWarehouseId?: string;
    quantityOrdered: number;
    unitPrice: number;
    discountAmount?: number;
  }[];
}

// Cost Formula Profiles
export type FreightProrationMethod = 'VALUE_PROPORTIONAL' | 'WEIGHT_PROPORTIONAL';

export interface CustomVariableDefinition {
  name: string;
  label: string;
  defaultValue: number;
  scope?: 'PROFILE_DEFAULT' | 'PER_INVOICE_PROMPT';
}

export interface CostFormulaProfile {
  id: string;
  code: string;
  name: string;
  formulaExpression: string;
  freightProrationMethod: FreightProrationMethod;
  customVariables: Record<string, CustomVariableDefinition>;
  isDefault: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCostFormulaProfileRequest {
  code: string;
  name: string;
  formulaExpression: string;
  freightProrationMethod?: FreightProrationMethod;
  customVariables?: Record<string, CustomVariableDefinition>;
  isDefault?: boolean;
}

export interface EvaluateFormulaTestRequest {
  formulaExpression: string;
  variables: Record<string, number>;
}

export interface EvaluateFormulaTestResponse {
  result: number;
  variablesUsed: Record<string, number>;
  success: boolean;
  errorMessage?: string | null;
}

// Inbound Invoices
export type InboundInvoiceStatus = 'DRAFT' | 'PENDING_CONFERENCE' | 'CONFERRED' | 'STOCKED' | 'CANCELLED';

export interface InboundInvoiceItem {
  id: string;
  itemIndex: number;
  supplierItemCode: string;
  supplierItemDescription: string;
  matchedProductId?: string | null;
  matchedProductName?: string | null;
  matchedProductSku?: string | null;
  supplierUom: string;
  internalUom: string;
  conversionFactor: number;
  invoicedQuantity: number;
  internalQuantity: number;
  unitPrice: number;
  totalPrice: number;
  allocatedFreight: number;
  allocatedInsurance: number;
  allocatedExpenses: number;
  icmsAmount: number;
  icmsCreditAmount: number;
  ipiAmount: number;
  pisAmount: number;
  pisCreditAmount: number;
  cofinsAmount: number;
  cofinsCreditAmount: number;
  finalLandedUnitCost: number;
  finalTotalCost: number;
}

export interface InboundInvoice {
  id: string;
  branchId: string;
  supplierId: string;
  supplierName?: string;
  costFormulaProfileId?: string | null;
  costFormulaProfileName?: string | null;
  purchaseOrderId?: string | null;
  accessKey?: string | null;
  invoiceNumber: string;
  series: string;
  issuanceDate: string;
  entryDate: string;
  totalProducts: number;
  totalFreight: number;
  totalInsurance: number;
  totalExpenses: number;
  totalDiscount: number;
  totalInvoiceAmount: number;
  customVariableValues?: Record<string, number>;
  status: InboundInvoiceStatus;
  items: InboundInvoiceItem[];
  createdAt: string;
  updatedAt: string;
}

export interface MatchProductRequest {
  productId: string;
  conversionFactor: number;
  supplierBarcode?: string;
}

export interface FinalizeInvoiceStockRequest {
  warehouseId: string;
  conferenceNotes?: string;
}

// Goods Receipts
export type GoodsReceiptStatus = 'IN_PROGRESS' | 'CONFERRED_OK' | 'CONFERRED_WITH_DIVERGENCE' | 'CANCELLED';

export interface GoodsReceiptItem {
  id?: string;
  productId: string;
  productName?: string;
  productSku?: string;
  expectedQuantity: number;
  receivedQuantity: number;
  rejectedQuantity: number;
  hasDivergence?: boolean;
  lotNumber?: string;
  fabricationDate?: string;
  expirationDate?: string;
  serialNumbers?: string[];
}

export interface GoodsReceipt {
  id: string;
  branchId: string;
  warehouseId: string;
  warehouseName?: string;
  purchaseOrderId?: string | null;
  inboundInvoiceId?: string | null;
  receiptNumber: string;
  receiptDate: string;
  status: GoodsReceiptStatus;
  conferenceNotes?: string | null;
  items: GoodsReceiptItem[];
  createdAt: string;
}

// Stock Transfers
export type StockTransferType = 'INTERNAL_WAREHOUSE' | 'INTER_BRANCH';
export type StockTransferStatus = 'DRAFT' | 'DISPATCHED_IN_TRANSIT' | 'RECEIVED_COMPLETED' | 'CANCELLED';

export interface StockTransferItem {
  id?: string;
  productId: string;
  productName?: string;
  productSku?: string;
  requestedQuantity: number;
  shippedQuantity?: number;
  receivedQuantity?: number;
  unitCost?: number;
  lotId?: string | null;
  lotNumber?: string | null;
  serialNumbers?: string[];
}

export interface StockTransfer {
  id: string;
  transferNumber: string;
  transferType: StockTransferType;
  originBranchId: string;
  originBranchName?: string;
  originWarehouseId: string;
  originWarehouseName?: string;
  destinationBranchId: string;
  destinationBranchName?: string;
  destinationWarehouseId: string;
  destinationWarehouseName?: string;
  status: StockTransferStatus;
  dispatchedAt?: string | null;
  receivedAt?: string | null;
  trackingNotes?: string | null;
  items: StockTransferItem[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateTransferRequest {
  transferNumber?: string;
  transferType: StockTransferType;
  originBranchId: string;
  originWarehouseId: string;
  destinationBranchId: string;
  destinationWarehouseId: string;
  trackingNotes?: string;
  items: {
    productId: string;
    quantity: number;
    lotId?: string;
    serialNumbers?: string[];
  }[];
}
