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

export async function getTransactionSummary(): Promise<TransactionSummary[]> {
  const { data } = await apiClient.get<TransactionSummary[]>(
    '/api/transactions/summary'
  );
  return data;
}

export async function getCategorySummary(): Promise<CategorySummary[]> {
  const { data } = await apiClient.get<CategorySummary[]>(
    '/api/transactions/categories'
  );
  return data;
}
