import { apiClient } from './client';

export interface StressTrendPoint {
  month: string;
  score: number | null;
}

export interface StressComponents {
  spendToIncomeRatio: number;
  savingsRate: number;
  recurringBurden: number;
  discretionaryGrowth: number;
  budgetAdherence: number;
}

export interface StressScoreResponse {
  month: string;
  score: number;
  label: 'Healthy' | 'Moderate' | 'Elevated' | 'High';
  explanation: string;
  trend: StressTrendPoint[];
  components: StressComponents;
}

export interface SimulateAdjustment {
  type: 'reduce_category' | 'add_recurring' | 'remove_recurring';
  category?: string;
  pct?: number;
  amount?: number;
  label?: string;
}

export interface SimulateRequest {
  monthYear: string;
  adjustments: SimulateAdjustment[];
}

export interface SimulateResponse {
  baseScore: number;
  projectedScore: number;
  scoreDelta: number;
  baseBalance: number;
  projectedBalance: number;
  balanceDelta: number;
  projectedSpend: number;
  adjustmentSummary: string;
}

const stressScoreApi = {
  getStressScore: async (monthYear: string): Promise<StressScoreResponse> => {
    const res = await apiClient.get<StressScoreResponse>('/api/stress-score', {
      params: { monthYear },
    });
    return res.data;
  },

  simulate: async (request: SimulateRequest): Promise<SimulateResponse> => {
    const res = await apiClient.post<SimulateResponse>('/api/stress-score/simulate', request);
    return res.data;
  },
};

export default stressScoreApi;
