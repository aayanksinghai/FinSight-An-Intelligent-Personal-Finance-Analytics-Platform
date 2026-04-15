import { apiClient } from './client';

export interface StressDistribution {
    averageScore: number;
    totalUsersScored: number;
    distribution: { name: string; value: number }[];
}

export const fetchTotalUsers = async () => {
    const res = await apiClient.get('/api/admin/metrics/users');
    return res.data;
};

export const fetchTotalTransactions = async () => {
    const res = await apiClient.get('/api/admin/metrics/transactions');
    return res.data;
};

export const fetchStressDistribution = async (): Promise<StressDistribution> => {
    const res = await apiClient.get('/api/stress-score/admin/distribution');
    return res.data;
};

export const fetchIngestionStats = async () => {
    const res = await apiClient.get('/api/admin/ingestion/stats');
    return res.data;
};

export const fetchAdminConfigs = async () => {
    const res = await apiClient.get('/api/admin/config');
    return res.data;
};

export const updateAdminConfigs = async (configs: Record<string, string>) => {
    const res = await apiClient.post('/api/admin/config', configs);
    return res.data;
};

export const triggerModelRetrain = async (modelName: string) => {
    const res = await apiClient.post(`/api/admin/models/retrain/${modelName}`);
    return res.data;
};
