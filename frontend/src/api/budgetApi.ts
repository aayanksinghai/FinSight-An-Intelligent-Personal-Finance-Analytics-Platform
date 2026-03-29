import axios from 'axios';

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
  const { data } = await axios.get<Budget[]>('/api/budgets', { params: { monthYear } });
  return data;
}

export async function upsertBudget(request: BudgetRequest): Promise<Budget> {
  const { data } = await axios.post<Budget>('/api/budgets', request);
  return data;
}
