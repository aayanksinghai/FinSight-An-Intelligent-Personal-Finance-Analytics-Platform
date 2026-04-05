import { apiClient } from './client';

export interface Budget {
  id: string;
  categoryId: string;
  categoryName: string;
  limitAmount: number;
  currentSpend: number;
  monthYear: string;
}

export interface BudgetRequest {
  categoryId: string;
  categoryName: string;
  limitAmount: number;
  monthYear: string;
}

export async function getBudgetsForMonth(monthYear: string): Promise<Budget[]> {
  const { data } = await apiClient.get<Budget[]>('/api/budgets', { params: { monthYear } });
  return data;
}

export async function upsertBudget(request: BudgetRequest): Promise<Budget> {
  const { data } = await apiClient.post<Budget>('/api/budgets', request);
  return data;
}

export async function deleteBudget(id: string): Promise<void> {
  await apiClient.delete(`/api/budgets/${id}`);
}
