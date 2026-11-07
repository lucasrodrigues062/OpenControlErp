import axios from 'axios';
import {
  Product,
  ProductPageResponse,
  CreateProductRequest,
  UpdateProductRequest,
  ProductCategory,
  CreateProductCategoryRequest,
  UpdateProductCategoryRequest,
  UnitConversionResponse,
  UnitConversionRequest,
  PriceList,
  CreatePriceListRequest,
  ProductPrice,
  SetProductPriceRequest,
} from '../types/catalog';

const api = axios.create({
  baseURL: '/api/v1/catalog',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('opencontrol_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const catalogApi = {
  // Products
  async searchProducts(params?: {
    query?: string;
    categoryId?: string;
    ncm?: string;
    trackingMode?: string;
    page?: number;
    size?: number;
  }): Promise<ProductPageResponse> {
    const res = await api.get<ProductPageResponse>('/products', { params });
    return res.data;
  },

  async getProductById(id: string): Promise<Product> {
    const res = await api.get<Product>(`/products/${id}`);
    return res.data;
  },

  async getProductByBarcode(barcode: string): Promise<Product> {
    const res = await api.get<Product>(`/products/by-barcode/${encodeURIComponent(barcode)}`);
    return res.data;
  },

  async createProduct(data: CreateProductRequest): Promise<Product> {
    const res = await api.post<Product>('/products', data);
    return res.data;
  },

  async updateProduct(id: string, data: UpdateProductRequest): Promise<Product> {
    const res = await api.put<Product>(`/products/${id}`, data);
    return res.data;
  },

  async deleteProduct(id: string): Promise<void> {
    await api.delete(`/products/${id}`);
  },

  async addUnitConversion(productId: string, data: UnitConversionRequest): Promise<UnitConversionResponse> {
    const res = await api.post<UnitConversionResponse>(`/products/${productId}/unit-conversions`, data);
    return res.data;
  },

  async listUnitConversions(productId: string): Promise<UnitConversionResponse[]> {
    const res = await api.get<UnitConversionResponse[]>(`/products/${productId}/unit-conversions`);
    return res.data;
  },

  // Categories
  async listCategories(params?: {
    parentId?: string;
    activeOnly?: boolean;
  }): Promise<ProductCategory[]> {
    const res = await api.get<ProductCategory[]>('/categories', { params });
    return res.data;
  },

  async getCategoryById(id: string): Promise<ProductCategory> {
    const res = await api.get<ProductCategory>(`/categories/${id}`);
    return res.data;
  },

  async createCategory(data: CreateProductCategoryRequest): Promise<ProductCategory> {
    const res = await api.post<ProductCategory>('/categories', data);
    return res.data;
  },

  async updateCategory(id: string, data: UpdateProductCategoryRequest): Promise<ProductCategory> {
    const res = await api.put<ProductCategory>(`/categories/${id}`, data);
    return res.data;
  },

  async deleteCategory(id: string): Promise<void> {
    await api.delete(`/categories/${id}`);
  },

  // Price Lists
  async listPriceLists(): Promise<PriceList[]> {
    const res = await api.get<PriceList[]>('/price-lists');
    return res.data;
  },

  async createPriceList(data: CreatePriceListRequest): Promise<PriceList> {
    const res = await api.post<PriceList>('/price-lists', data);
    return res.data;
  },

  async setProductPrice(priceListId: string, productId: string, data: SetProductPriceRequest): Promise<ProductPrice> {
    const res = await api.put<ProductPrice>(`/price-lists/${priceListId}/prices/${productId}`, data);
    return res.data;
  },

  async getProductPrices(productId: string): Promise<ProductPrice[]> {
    const res = await api.get<ProductPrice[]>(`/products/${productId}/prices`);
    return res.data;
  },
};
