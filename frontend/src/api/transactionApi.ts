import { apiClient } from './client';
import type { PageResponse } from '../types/ingestion';
import type { 
  TransactionResponse, 
  TransactionSummary, 
  CategorySummary 
} from '../types/transaction';

export async function getTransactions(
  page = 0,
  size = 20,
): Promise<PageResponse<TransactionResponse>> {
  const { data } = await apiClient.get<PageResponse<TransactionResponse>>(
    '/api/transactions',
    { params: { page, size } },
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
