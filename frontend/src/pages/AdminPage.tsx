import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import {
  fetchTotalUsers,
  fetchTotalTransactions,
  fetchStressDistribution,
  fetchIngestionStats,
  fetchAdminConfigs,
  updateAdminConfigs,
  triggerModelRetrain,
  StressDistribution
} from '../api/adminApi';
import forecastApi, { AccuracyResponse } from '../api/forecastApi';

export default function AdminPage() {
  const [loading, setLoading] = useState(true);
  const [totalUsers, setTotalUsers] = useState<number>(0);
  const [totalTxns, setTotalTxns] = useState<number>(0);
  const [accuracy, setAccuracy] = useState<AccuracyResponse | null>(null);
  const [stressDist, setStressDist] = useState<StressDistribution | null>(null);
  const [ingestionStats, setIngestionStats] = useState<any>(null);
  const [configs, setConfigs] = useState<Record<string, string>>({});
  const [isConfigSaving, setIsConfigSaving] = useState(false);

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    setLoading(true);
    try {
      const [users, txns, dist, ing, conf, acc] = await Promise.all([
        fetchTotalUsers().catch(() => ({ totalUsers: 0 })),
        fetchTotalTransactions().catch(() => ({ totalTransactions: 0 })),
        fetchStressDistribution().catch(() => null),
        fetchIngestionStats().catch(() => ({ totalJobs: 0, successRate: 0 })),
        fetchAdminConfigs().catch(() => ({})),
        forecastApi.getAccuracy().catch(() => null)
      ]);

      setTotalUsers(users.totalUsers);
      setTotalTxns(txns.totalTransactions);
      setStressDist(dist);
      setIngestionStats(ing);
      setConfigs(conf);
      setAccuracy(acc);
    } catch (err) {
      toast.error('Failed to load some admin data');
    } finally {
      setLoading(false);
    }
  };

  const handleConfigChange = (key: string, value: string) => {
    setConfigs({ ...configs, [key]: value });
  };

  const saveConfigs = async () => {
    setIsConfigSaving(true);
    try {
      await updateAdminConfigs(configs);
      toast.success('ML Thresholds updated globally!');
    } catch (err) {
      toast.error('Failed to save configs');
    } finally {
      setIsConfigSaving(false);
    }
  };

  const fireRetrain = async (model: string) => {
    const p = toast.loading(`Triggering ${model} retrain...`);
    try {
      const res = await triggerModelRetrain(model);
      toast.success(res.message, { id: p });
    } catch (err: any) {
      toast.error(err?.response?.data?.detail || 'Failed to trigger retrain', { id: p });
    }
  };

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <span className="spinner"></span>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 animate-fade-in pb-8">
      <div className="flex items-center justify-between border-b border-white/10 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-[#edf2ff]">Platform Administration</h1>
          <p className="text-sm text-muted">Global analytics, ML thresholds, and job monitors.</p>
        </div>
        <button className="btn-primary" onClick={loadDashboard}>
          ↺ Refresh
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="kpi-card bg-brand/10 border border-brand/20">
          <h3 className="kpi-label">Total Users</h3>
          <p className="kpi-value text-blue-400">{totalUsers.toLocaleString()}</p>
        </div>
        <div className="kpi-card bg-brand/10 border border-brand/20">
          <h3 className="kpi-label">Transactions Ingested</h3>
          <p className="kpi-value text-emerald-400">{totalTxns.toLocaleString()}</p>
        </div>
        <div className="kpi-card bg-brand/10 border border-brand/20">
          <h3 className="kpi-label">Ingestion Success</h3>
          <p className="kpi-value text-purple-400">{ingestionStats?.successRate || 0}%</p>
          <p className="text-xs text-muted mt-1">out of {ingestionStats?.totalJobs || 0} jobs</p>
        </div>
        <div className="kpi-card bg-brand/10 border border-brand/20">
          <h3 className="kpi-label">Global Forecasting MAPE</h3>
          <p className="kpi-value text-amber-400">{accuracy?.mape || 'N/A'}</p>
          <p className="text-xs text-muted mt-1">{accuracy ? `${accuracy.totalSnapshots} eval points` : ''}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="glass-card flex flex-col gap-4">
          <h2 className="text-lg font-semibold text-white">Dynamic ML Thresholds</h2>
          <p className="text-sm text-muted mb-2">Changes are applied immediately without container redeployment.</p>
          
          <div className="space-y-4">
            {Object.keys(configs).length === 0 ? (
              <p className="text-sm text-muted">No configurations found.</p>
            ) : (
              Object.entries(configs)
                .sort(([a], [b]) => a.localeCompare(b))
                .map(([key, val]) => (
                <div key={key} className="flex items-center justify-between bg-white/5 p-3 rounded-lg border border-white/10">
                  <div className="flex-1">
                    <p className="text-sm font-medium text-white font-mono">{key}</p>
                  </div>
                  <div className="w-1/3">
                    <input 
                      type="number" 
                      step="0.01" 
                      className="form-input w-full text-right"
                      value={val}
                      onChange={e => handleConfigChange(key, e.target.value)}
                    />
                  </div>
                </div>
              ))
            )}
          </div>
          <button 
            className="btn-primary self-end mt-4" 
            onClick={saveConfigs} 
            disabled={isConfigSaving || Object.keys(configs).length === 0}
          >
            {isConfigSaving ? 'Saving...' : 'Save Configuration'}
          </button>
        </div>

        <div className="flex flex-col gap-6">
          <div className="glass-card">
            <h2 className="text-lg font-semibold text-white mb-4">Manual Model Retraining</h2>
            <div className="grid grid-cols-2 gap-3">
              {['forecasting', 'stress_score', 'anomaly_detection', 'categorization'].map(model => (
                <button 
                  key={model}
                  onClick={() => fireRetrain(model)}
                  className="btn-ghost bg-white/5 hover:bg-emerald-500/20 border border-white/10 hover:border-emerald-500/50 justify-center h-16 flex flex-col items-center"
                >
                  <span className="font-mono text-xs">{model}</span>
                  <span className="text-emerald-400 text-xs mt-1">» Trigger DAG</span>
                </button>
              ))}
            </div>
          </div>

          <div className="glass-card flex-1">
            <h2 className="text-lg font-semibold text-white mb-4">Global Stress Distribution</h2>
            {stressDist ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-muted">Avg Network Score</span>
                  <span className="font-semibold text-xl text-white">{stressDist.averageScore.toFixed(1)}</span>
                </div>
                <div className="space-y-3">
                  {stressDist.distribution.map(d => (
                    <div key={d.name} className="flex items-center justify-between text-sm">
                      <span className="w-24 text-muted">{d.name}</span>
                      <div className="flex-1 mx-4 bg-white/10 rounded-full h-2 overflow-hidden">
                        <div 
                           className={`h-full ${d.name === 'High' ? 'bg-red-500' : d.name === 'Elevated' ? 'bg-amber-500' : d.name === 'Moderate' ? 'bg-yellow-400' : 'bg-emerald-500'}`}
                           style={{ width: `${stressDist.totalUsersScored > 0 ? (d.value / stressDist.totalUsersScored) * 100 : 0}%` }}
                        />
                      </div>
                      <span className="w-8 text-right font-mono">{d.value}</span>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
               <p className="text-sm text-muted">Stress data unavailable</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
