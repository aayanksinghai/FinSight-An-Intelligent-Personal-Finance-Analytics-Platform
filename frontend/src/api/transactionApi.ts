import { apiClient } from './client';
import type { PageResponse } from '../types/ingestion';
import type { 
  TransactionResponse, 
  TransactionSummary, 
  CategorySummary 
} from '../types/transaction';

export interface TransactionFilters {
  search?: string;
  category?: string;
  type?: 'DEBIT' | 'CREDIT' | '';
  from?: string;
  to?: string;
}

export async function getTransactions(
  page = 0,
  size = 20,
  filters: TransactionFilters = {},
): Promise<PageResponse<TransactionResponse>> {
  const params: Record<string, unknown> = { page, size };
  if (filters.search) params.search = filters.search;
  if (filters.category) params.category = filters.category;
  if (filters.type) params.type = filters.type;
  if (filters.from) params.from = filters.from;
  if (filters.to) params.to = filters.to;

  const { data } = await apiClient.get<PageResponse<TransactionResponse>>(
    '/api/transactions',
    { params },
  );
  return data;
}

export async function getTransactionSummary(monthYear?: string): Promise<TransactionSummary[]> {
  const params: Record<string, string> = {};
  if (monthYear) {
    const year = parseInt(monthYear.split('-')[0]);
    const month = parseInt(monthYear.split('-')[1]);
    const fromDate = new Date(Date.UTC(year, month - 1, 1));
    const toDate = new Date(Date.UTC(year, month, 0, 23, 59, 59, 999));
    params.from = fromDate.toISOString();
    params.to = toDate.toISOString();
  }
  const { data } = await apiClient.get<TransactionSummary[]>(
    '/api/transactions/summary', { params }
  );
  return data;
}

export async function getCategorySummary(monthYear?: string): Promise<CategorySummary[]> {
  const params: Record<string, string> = {};
  if (monthYear) {
    const year = parseInt(monthYear.split('-')[0]);
    const month = parseInt(monthYear.split('-')[1]);
    const fromDate = new Date(Date.UTC(year, month - 1, 1));
    const toDate = new Date(Date.UTC(year, month, 0, 23, 59, 59, 999));
    params.from = fromDate.toISOString();
    params.to = toDate.toISOString();
  }
  const { data } = await apiClient.get<CategorySummary[]>(
    '/api/transactions/categories', { params }
  );
  return data;
}
