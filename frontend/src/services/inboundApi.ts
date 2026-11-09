import axios from 'axios';
import {
  PurchaseOrder,
  CreatePurchaseOrderRequest,
  CostFormulaProfile,
  CreateCostFormulaProfileRequest,
  EvaluateFormulaTestRequest,
  EvaluateFormulaTestResponse,
  InboundInvoice,
  MatchProductRequest,
  FinalizeInvoiceStockRequest,
  GoodsReceipt,
  StockTransfer,
  CreateTransferRequest,
} from '../types/inbound';

const api = axios.create({
  baseURL: '/api/v1/inbound',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('opencontrol_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const inboundApi = {
  // Cost Formula Profiles
  async listCostFormulaProfiles(): Promise<CostFormulaProfile[]> {
    const res = await api.get<CostFormulaProfile[]>('/cost-formula-profiles');
    return res.data;
  },

  async createCostFormulaProfile(data: CreateCostFormulaProfileRequest): Promise<CostFormulaProfile> {
    const res = await api.post<CostFormulaProfile>('/cost-formula-profiles', data);
    return res.data;
  },

  async evaluateFormulaTest(data: EvaluateFormulaTestRequest): Promise<EvaluateFormulaTestResponse> {
    const res = await api.post<EvaluateFormulaTestResponse>('/cost-formula-profiles/evaluate-test', data);
    return res.data;
  },

  async setDefaultCostFormulaProfile(id: string): Promise<CostFormulaProfile> {
    const res = await api.put<CostFormulaProfile>(`/cost-formula-profiles/${id}/default`);
    return res.data;
  },

  // Purchase Orders
  async listPurchaseOrders(params?: { status?: string; supplierId?: string }): Promise<PurchaseOrder[]> {
    const res = await api.get<PurchaseOrder[]>('/purchase-orders', { params });
    return res.data;
  },

  async getPurchaseOrderById(id: string): Promise<PurchaseOrder> {
    const res = await api.get<PurchaseOrder>(`/purchase-orders/${id}`);
    return res.data;
  },

  async createPurchaseOrder(data: CreatePurchaseOrderRequest): Promise<PurchaseOrder> {
    const res = await api.post<PurchaseOrder>('/purchase-orders', data);
    return res.data;
  },

  async approvePurchaseOrder(id: string): Promise<PurchaseOrder> {
    const res = await api.post<PurchaseOrder>(`/purchase-orders/${id}/approve`);
    return res.data;
  },

  async cancelPurchaseOrder(id: string): Promise<PurchaseOrder> {
    const res = await api.post<PurchaseOrder>(`/purchase-orders/${id}/cancel`);
    return res.data;
  },

  // Inbound Invoices
  async listInboundInvoices(params?: { status?: string; branchId?: string }): Promise<InboundInvoice[]> {
    const res = await api.get<InboundInvoice[]>('/invoices', { params });
    return res.data;
  },

  async getInboundInvoiceById(id: string): Promise<InboundInvoice> {
    const res = await api.get<InboundInvoice>(`/invoices/${id}`);
    return res.data;
  },

  async uploadNFeXml(formData: FormData, autoRegisterMissingProducts: boolean = false): Promise<InboundInvoice> {
    const res = await api.post<InboundInvoice>('/invoices/upload-xml', formData, {
      params: { autoRegisterMissingProducts },
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },

  async autoRegisterMissingProducts(invoiceId: string): Promise<InboundInvoice> {
    const res = await api.post<InboundInvoice>(`/invoices/${invoiceId}/auto-register-missing-products`);
    return res.data;
  },

  async matchInvoiceItem(invoiceId: string, itemId: string, data: MatchProductRequest): Promise<InboundInvoice> {
    const res = await api.put<InboundInvoice>(`/invoices/${invoiceId}/items/${itemId}/match`, data);
    return res.data;
  },

  async finalizeInvoiceStock(invoiceId: string, data: FinalizeInvoiceStockRequest): Promise<InboundInvoice> {
    const res = await api.post<InboundInvoice>(`/invoices/${invoiceId}/finalize-stock`, data);
    return res.data;
  },

  // Goods Receipts
  async listGoodsReceipts(): Promise<GoodsReceipt[]> {
    const res = await api.get<GoodsReceipt[]>('/goods-receipts');
    return res.data;
  },

  async getGoodsReceiptById(id: string): Promise<GoodsReceipt> {
    const res = await api.get<GoodsReceipt>(`/goods-receipts/${id}`);
    return res.data;
  },

  // Stock Transfers
  async listTransfers(params?: { type?: string; status?: string }): Promise<StockTransfer[]> {
    const res = await api.get<StockTransfer[]>('/transfers', { params });
    return res.data;
  },

  async getTransferById(id: string): Promise<StockTransfer> {
    const res = await api.get<StockTransfer>(`/transfers/${id}`);
    return res.data;
  },

  async createTransfer(data: CreateTransferRequest): Promise<StockTransfer> {
    const res = await api.post<StockTransfer>('/transfers', data);
    return res.data;
  },

  async dispatchTransfer(id: string): Promise<StockTransfer> {
    const res = await api.post<StockTransfer>(`/transfers/${id}/dispatch`);
    return res.data;
  },

  async receiveTransfer(id: string): Promise<StockTransfer> {
    const res = await api.post<StockTransfer>(`/transfers/${id}/receive`);
    return res.data;
  },
};
