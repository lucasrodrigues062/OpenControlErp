import axios from 'axios';
import {
  Warehouse,
  CreateWarehouseRequest,
  UpdateWarehouseRequest,
  StorageLocation,
  CreateStorageLocationRequest,
  ProductLot,
  CreateLotRequest,
  ProductSerialNumber,
  CreateSerialNumberRequest,
  StockBalance,
  StockMovement,
  CreateStockMovementRequest,
  StockMovementPageResponse,
} from '../types/inventory';

const api = axios.create({
  baseURL: '/api/v1/inventory',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('opencontrol_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const inventoryApi = {
  // Warehouses
  async listWarehouses(branchId?: string): Promise<Warehouse[]> {
    const res = await api.get<Warehouse[]>('/warehouses', {
      params: branchId ? { branchId } : undefined,
    });
    return res.data;
  },

  async getWarehouseById(id: string): Promise<Warehouse> {
    const res = await api.get<Warehouse>(`/warehouses/${id}`);
    return res.data;
  },

  async createWarehouse(data: CreateWarehouseRequest): Promise<Warehouse> {
    const res = await api.post<Warehouse>('/warehouses', data);
    return res.data;
  },

  async updateWarehouse(id: string, data: UpdateWarehouseRequest): Promise<Warehouse> {
    const res = await api.put<Warehouse>(`/warehouses/${id}`, data);
    return res.data;
  },

  async deleteWarehouse(id: string): Promise<void> {
    await api.delete(`/warehouses/${id}`);
  },

  // Storage Locations
  async listLocations(warehouseId: string): Promise<StorageLocation[]> {
    const res = await api.get<StorageLocation[]>(`/warehouses/${warehouseId}/locations`);
    return res.data;
  },

  async createLocation(warehouseId: string, data: CreateStorageLocationRequest): Promise<StorageLocation> {
    const res = await api.post<StorageLocation>(`/warehouses/${warehouseId}/locations`, data);
    return res.data;
  },

  // Stock Balances
  async listBalances(params?: {
    warehouseId?: string;
    productId?: string;
  }): Promise<StockBalance[]> {
    const res = await api.get<StockBalance[]>('/balances', { params });
    return res.data;
  },

  async getProductBalances(productId: string): Promise<StockBalance[]> {
    const res = await api.get<StockBalance[]>(`/balances/products/${productId}`);
    return res.data;
  },

  // Traceability - Lots
  async listLots(params: {
    productId: string;
    warehouseId?: string;
    availableOnly?: boolean;
  }): Promise<ProductLot[]> {
    const res = await api.get<ProductLot[]>('/lots', { params });
    return res.data;
  },

  async createLot(data: CreateLotRequest): Promise<ProductLot> {
    const res = await api.post<ProductLot>('/lots', data);
    return res.data;
  },

  // Traceability - Serial Numbers
  async listSerialNumbers(params?: {
    productId?: string;
    serialNumber?: string;
    status?: string;
  }): Promise<ProductSerialNumber[]> {
    const res = await api.get<ProductSerialNumber[]>('/serial-numbers', { params });
    return res.data;
  },

  async createSerialNumber(data: CreateSerialNumberRequest): Promise<ProductSerialNumber> {
    const res = await api.post<ProductSerialNumber>('/serial-numbers', data);
    return res.data;
  },

  // Kardex Movements
  async listMovements(params?: {
    productId?: string;
    warehouseId?: string;
    movementType?: string;
    page?: number;
    size?: number;
  }): Promise<StockMovementPageResponse> {
    const res = await api.get<StockMovementPageResponse>('/movements', { params });
    return res.data;
  },

  async createMovement(data: CreateStockMovementRequest): Promise<StockMovement> {
    const res = await api.post<StockMovement>('/movements', data);
    return res.data;
  },
};
