import { apiClient } from './client';

export interface ForecastResponse {
  category: string;
  predictedAmount: number;
  lowerBound: number;
  upperBound: number;
  actualAmountToDate: number | null;
}

export interface AccuracyResponse {
  mape: number;
  totalSnapshots: number;
  accuracyPercentage: number;
}

const forecastApi = {
  getForecast: async (monthYear: string): Promise<ForecastResponse[]> => {
    const response = await apiClient.get(`/api/forecast`, {
      params: { monthYear },
    });
    return response.data;
  },

  getAccuracy: async (): Promise<AccuracyResponse> => {
    const response = await apiClient.get(`/api/forecast/admin/accuracy`);
    return response.data;
  },
};


export default forecastApi;
