export type SpedItemType =
  | 'REV_00' // Mercadoria para Revenda
  | 'MP_01'  // Matéria-Prima
  | 'EMB_02' // Embalagem
  | 'PP_03'  // Produto em Processo
  | 'PA_04'  // Produto Acabado
  | 'SP_05'  // Subproduto
  | 'PI_06'  // Produto Intermediário
  | 'UC_07'  // Material de Uso e Consumo
  | 'AI_08'  // Ativo Imobilizado
  | 'SRV_09' // Serviços
  | 'OUT_10' // Outros Insumos
  | 'OUT_99'; // Outras

export type ProductTrackingMode = 'NONE' | 'BATCH_LOT' | 'SERIAL_NUMBER';

export interface ProductCategory {
  id: string;
  parentId?: string | null;
  code: string;
  name: string;
  colorHex: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductCategoryRequest {
  parentId?: string | null;
  code: string;
  name: string;
  colorHex?: string;
}

export interface UpdateProductCategoryRequest {
  parentId?: string | null;
  name: string;
  colorHex?: string;
  isActive?: boolean;
}

export interface UnitConversionResponse {
  id: string;
  productId: string;
  unitFrom: string;
  unitTo: string;
  conversionFactor: number;
}

export interface UnitConversionRequest {
  unitFrom: string;
  unitTo: string;
  conversionFactor: number;
}

export interface Product {
  id: string;
  sku: string;
  barcode?: string | null;
  name: string;
  description?: string | null;
  itemType: SpedItemType;
  unitOfMeasure: string;
  categoryId: string;
  categoryName?: string | null;
  brand?: string | null;
  model?: string | null;
  ncm: string;
  ncmException?: string | null;
  cest?: string | null;
  origin: number;
  trackingMode: ProductTrackingMode;
  grossWeight?: number | null;
  netWeight?: number | null;
  width?: number | null;
  height?: number | null;
  depth?: number | null;
  volume?: number | null;
  minStock: number;
  maxStock?: number | null;
  reorderPoint?: number | null;
  lastAcquisitionCost: number;
  averageCost: number;
  replacementCost: number;
  isActive: boolean;
  unitConversions: UnitConversionResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface ProductPageResponse {
  items: Product[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface CreateProductRequest {
  sku: string;
  barcode?: string | null;
  name: string;
  description?: string | null;
  itemType?: SpedItemType;
  unitOfMeasure: string;
  categoryId: string;
  brand?: string | null;
  model?: string | null;
  ncm: string;
  ncmException?: string | null;
  cest?: string | null;
  origin?: number;
  trackingMode?: ProductTrackingMode;
  grossWeight?: number | null;
  netWeight?: number | null;
  width?: number | null;
  height?: number | null;
  depth?: number | null;
  volume?: number | null;
  minStock?: number;
  maxStock?: number | null;
  reorderPoint?: number | null;
  lastAcquisitionCost?: number;
  branchId?: string | null;
}

export interface UpdateProductRequest {
  sku: string;
  barcode?: string | null;
  name: string;
  description?: string | null;
  itemType?: SpedItemType;
  unitOfMeasure: string;
  categoryId: string;
  brand?: string | null;
  model?: string | null;
  ncm: string;
  ncmException?: string | null;
  cest?: string | null;
  origin?: number;
  trackingMode?: ProductTrackingMode;
  grossWeight?: number | null;
  netWeight?: number | null;
  width?: number | null;
  height?: number | null;
  depth?: number | null;
  volume?: number | null;
  minStock?: number;
  maxStock?: number | null;
  reorderPoint?: number | null;
  lastAcquisitionCost?: number | null;
  isActive?: boolean;
}

export interface PriceList {
  id: string;
  code: string;
  name: string;
  currency: string;
  discountOrMarkupPct: number;
  isDefault: boolean;
  validFrom?: string | null;
  validUntil?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePriceListRequest {
  code: string;
  name: string;
  currency?: string;
  discountOrMarkupPct?: number;
  isDefault?: boolean;
  validFrom?: string | null;
  validUntil?: string | null;
}

export interface ProductPrice {
  id: string;
  priceListId: string;
  productId: string;
  salesPrice: number;
  promotionalPrice?: number | null;
  promoStart?: string | null;
  promoEnd?: string | null;
  updatedAt: string;
}

export interface SetProductPriceRequest {
  salesPrice: number;
  promotionalPrice?: number | null;
  promoStart?: string | null;
  promoEnd?: string | null;
}
