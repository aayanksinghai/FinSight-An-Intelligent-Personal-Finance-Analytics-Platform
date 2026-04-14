import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getTransactionSummary, getCategorySummary } from '../api/transactionApi';
import { useAuthStore } from '../store/authStore';
import { Link } from 'react-router-dom';
import {
  PieChart, Pie, Cell, Tooltip as RechartsTooltip, ResponsiveContainer, Legend,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, ErrorBar,
  LineChart, Line, ReferenceLine
} from 'recharts';
import { getBudgetsForMonth } from '../api/budgetApi';
import forecastApi from '../api/forecastApi';
import stressScoreApi from '../api/stressScoreApi';
import WhatIfSimulator from '../components/WhatIfSimulator';

function formatCurrency(amount: number) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(amount);
}

const SCORE_COLORS: Record<string, string> = {
  Healthy: '#22c55e',
  Moderate: '#f59e0b',
  Elevated: '#f97316',
  High: '#ef4444',
};

const SCORE_BG: Record<string, string> = {
  Healthy: 'rgba(34,197,94,0.08)',
  Moderate: 'rgba(245,158,11,0.08)',
  Elevated: 'rgba(249,115,22,0.08)',
  High: 'rgba(239,68,68,0.08)',
};

export default function DashboardPage() {
  const userEmail = useAuthStore((s) => s.userEmail);
  const [showSimulator, setShowSimulator] = useState(false);

  const { data: summaryData, isLoading: summaryLoading } = useQuery({
    queryKey: ['transaction-summary'],
    queryFn: () => getTransactionSummary(),
    refetchInterval: 10000,
  });

  // Use 2025-02 specifically as the anchor month for the sample dataset
  const currentMonth = '2025-02';

  const { data: budgetData } = useQuery({
    queryKey: ['budgets', currentMonth],
    queryFn: () => getBudgetsForMonth(currentMonth),
    refetchInterval: 10000,
  });

  const { data: forecastData } = useQuery({
    queryKey: ['forecast', currentMonth],
    queryFn: () => forecastApi.getForecast(currentMonth),
    refetchInterval: 10000,
  });

  const { data: categoryData, isLoading: categoryLoading } = useQuery({
    queryKey: ['category-summary'],
    queryFn: () => getCategorySummary(),
    refetchInterval: 10000,
  });

  const { data: stressData } = useQuery({
    queryKey: ['stress-score', currentMonth],
    queryFn: () => stressScoreApi.getStressScore(currentMonth),
    retry: false,
  });

  const income = summaryData?.find((s) => s.type === 'CREDIT')?.total ?? 0;
  const spend = summaryData?.find((s) => s.type === 'DEBIT')?.total ?? 0;

  const chartData = categoryData?.map((c) => ({
    name: c.category || 'Uncategorized',
    value: c.total,
    color: c.color || '#94A3B8'
  })) ?? [];

  const categories = Array.from(new Set([
    ...(budgetData?.map(b => b.categoryName) || []),
    ...(forecastData?.map(f => f.category) || [])
  ]));

  const combinedData = categories.map(cat => {
    const budget = budgetData?.find(b => b.categoryName === cat)?.limitAmount || 0;
    const forecast = forecastData?.find(f => f.category === cat);
    return {
      name: cat,
      Budget: budget,
      Predicted: forecast ? forecast.predictedAmount : 0,
      confidenceInterval: forecast
        ? [Math.max(0, forecast.predictedAmount - forecast.lowerBound), Math.max(0, forecast.upperBound - forecast.predictedAmount)]
        : [0, 0]
    };
  }).filter(d => d.Budget > 0 || d.Predicted > 0).slice(0, 5);

  // 6-month trend for stress score
  const trendData = stressData?.trend?.filter(t => t.score !== null).map(t => ({
    month: t.month.slice(0, 7),
    score: t.score,
  })) ?? [];

  const isLoading = summaryLoading || categoryLoading;
  const hasData = (income > 0 || spend > 0) && chartData.length > 0;

  const scoreColor = SCORE_COLORS[stressData?.label ?? 'Moderate'];
  const scoreBg = SCORE_BG[stressData?.label ?? 'Moderate'];

  return (
    <div className="flex flex-col gap-5 animate-fade-in">
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#edf2ff]">
            Welcome back, {userEmail ? userEmail.split('@')[0] : 'User'}
          </h1>
          <p className="mt-0.5 text-sm text-muted">
            Here's your financial overview.
          </p>
        </div>
        {/* What-If Simulator Toggle */}
        <button
          onClick={() => setShowSimulator(true)}
          style={{
            display: 'flex', alignItems: 'center', gap: 8,
            padding: '10px 18px',
            background: 'linear-gradient(135deg, rgba(99,102,241,0.15), rgba(129,140,248,0.1))',
            border: '1px solid rgba(99,102,241,0.3)',
            borderRadius: 10, color: '#818cf8', cursor: 'pointer',
            fontSize: 13, fontWeight: 600,
            transition: 'all 0.2s',
          }}
          onMouseEnter={e => (e.currentTarget.style.background = 'linear-gradient(135deg, rgba(99,102,241,0.25), rgba(129,140,248,0.2))')}
          onMouseLeave={e => (e.currentTarget.style.background = 'linear-gradient(135deg, rgba(99,102,241,0.15), rgba(129,140,248,0.1))')}
        >
          🧮 What-If Simulator
        </button>
      </div>

      {isLoading ? (
        <div className="flex h-64 items-center justify-center">
          <span className="spinner" />
        </div>
      ) : !hasData ? (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="glass-card flex flex-col items-center justify-center gap-4 p-8 md:col-span-2">
            <div className="flex h-12 w-12 items-center justify-center rounded-full border border-brand/30 bg-brand/10 text-2xl">
              📊
            </div>
            <p className="text-sm font-semibold text-[#edf2ff]">Your charts will appear here after you upload a statement</p>
            <p className="max-w-sm text-center text-xs text-muted">
              Monthly spend breakdown, category pie chart, and top merchants —
              all powered by Recharts and your real transaction data.
            </p>
            <Link to="/upload" className="btn-primary no-underline hover:no-underline">
              ↑ Upload Your First Statement
            </Link>
          </div>
        </div>
      ) : (
        <>
          {/* KPI Cards — includes Stress Score */}
          <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
            <div className="kpi-card">
              <h3 className="kpi-label">Total Spent</h3>
              <p className="kpi-value text-danger">{formatCurrency(spend)}</p>
            </div>
            <div className="kpi-card">
              <h3 className="kpi-label">Total Income</h3>
              <p className="kpi-value text-success">{formatCurrency(income)}</p>
            </div>
            <div className="kpi-card">
              <h3 className="kpi-label">Net Flow</h3>
              <p className={`kpi-value ${income - spend >= 0 ? 'text-success' : 'text-danger'}`}>
                {income - spend >= 0 ? '+' : ''}{formatCurrency(income - spend)}
              </p>
            </div>

            {/* Financial Stress Score KPI */}
            {stressData ? (
              <div
                className="kpi-card"
                style={{ background: scoreBg, border: `1px solid ${scoreColor}33` }}
              >
                <h3 className="kpi-label">Financial Stress Score</h3>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
                  <p className="kpi-value" style={{ color: scoreColor }}>{stressData.score}</p>
                  <span style={{ fontSize: 12, color: scoreColor, fontWeight: 600 }}>
                    / 100 · {stressData.label}
                  </span>
                </div>
              </div>
            ) : (
              <div className="kpi-card">
                <h3 className="kpi-label">Financial Stress Score</h3>
                <p className="kpi-value text-muted">–</p>
              </div>
            )}
          </div>

          {/* Stress Score Panel — trend + explanation */}
          {stressData && (
            <div
              style={{
                borderRadius: 16, padding: 24,
                background: 'linear-gradient(135deg, #13172d, #1a1f38)',
                border: `1px solid ${scoreColor}22`,
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20, flexWrap: 'wrap', gap: 12 }}>
                <div>
                  <h2 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: '#edf2ff' }}>
                    📈 Stress Score — 6 Month Trend
                  </h2>
                  <p style={{ margin: '4px 0 0', fontSize: 12, color: '#64748b' }}>
                    Lower is better · Score ranges: 0–29 Healthy · 30–54 Moderate · 55–74 Elevated · 75+ High
                  </p>
                </div>
                <div style={{
                  padding: '8px 16px', borderRadius: 20,
                  background: `${scoreColor}18`, border: `1px solid ${scoreColor}44`,
                  fontSize: 13, fontWeight: 700, color: scoreColor,
                }}>
                  {stressData.label}
                </div>
              </div>

              {trendData.length > 1 ? (
                <ResponsiveContainer width="100%" height={180}>
                  <LineChart data={trendData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#1e2540" vertical={false} />
                    <XAxis dataKey="month" stroke="#64748b" tick={{ fill: '#94a3b8', fontSize: 11 }} />
                    <YAxis domain={[0, 100]} stroke="#64748b" tick={{ fill: '#94a3b8', fontSize: 11 }} />
                    <RechartsTooltip
                      formatter={(v: number) => [v, 'Stress Score']}
                      contentStyle={{ backgroundColor: '#1a1f33', borderColor: '#2e3650', borderRadius: '8px' }}
                      itemStyle={{ color: '#edf2ff' }}
                    />
                    <ReferenceLine y={30} stroke="#22c55e" strokeDasharray="4 2" strokeOpacity={0.4} />
                    <ReferenceLine y={55} stroke="#f59e0b" strokeDasharray="4 2" strokeOpacity={0.4} />
                    <ReferenceLine y={75} stroke="#ef4444" strokeDasharray="4 2" strokeOpacity={0.4} />
                    <Line
                      type="monotone" dataKey="score"
                      stroke={scoreColor} strokeWidth={3}
                      dot={{ fill: scoreColor, r: 4, strokeWidth: 2 }}
                      activeDot={{ r: 6, fill: scoreColor }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              ) : (
                <div style={{ textAlign: 'center', padding: '32px 0', color: '#64748b', fontSize: 13 }}>
                  Not enough historical data yet — trends will appear after 2+ months of data.
                </div>
              )}

              {/* AI Explanation */}
              <div style={{
                marginTop: 16, padding: '14px 18px',
                background: 'rgba(255,255,255,0.03)', borderRadius: 12,
                borderLeft: `3px solid ${scoreColor}`,
              }}>
                <p style={{ margin: 0, fontSize: 13, color: '#cbd5e1', lineHeight: 1.7 }}>
                  💡 {stressData.explanation}
                </p>
              </div>
            </div>
          )}

          {/* Charts Row */}
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div className="glass-card flex flex-col p-5 h-96">
              <h2 className="mb-4 text-base font-semibold text-[#edf2ff]">Spend by Category</h2>
              <div className="flex-1">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={chartData}
                      cx="50%"
                      cy="50%"
                      innerRadius={80}
                      outerRadius={120}
                      paddingAngle={2}
                      dataKey="value"
                      stroke="none"
                    >
                      {chartData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <RechartsTooltip
                      formatter={(value: number) => formatCurrency(value)}
                      contentStyle={{ backgroundColor: '#1a1f33', borderColor: '#2e3650', borderRadius: '8px' }}
                      itemStyle={{ color: '#edf2ff' }}
                    />
                    <Legend verticalAlign="bottom" height={36} wrapperStyle={{ fontSize: '12px' }}/>
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="glass-card flex flex-col p-5 h-96">
              <h2 className="mb-4 text-base font-semibold text-[#edf2ff]">Top Categories</h2>
              <div className="flex flex-col gap-4 overflow-y-auto">
                {categoryData?.map((cat, idx) => (
                  <div key={idx} className="flex items-center justify-between border-b border-stroke/50 pb-3 last:border-0 last:pb-0">
                    <div className="flex items-center gap-3">
                      <div
                        className="h-3 w-3 rounded-full"
                        style={{ backgroundColor: cat.color || '#94A3B8' }}
                      />
                      <span className="text-sm text-[#edf2ff]">{cat.category || 'Uncategorized'}</span>
                    </div>
                    <span className="font-medium text-[#edf2ff]">{formatCurrency(cat.total)}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Forecast vs Budget Chart */}
            <div className="glass-card flex flex-col p-5 h-96 md:col-span-2">
              <div className="mb-4">
                <h2 className="text-base font-semibold text-[#edf2ff]">AI Spend Forecast</h2>
                <p className="text-xs text-muted">Predicted spending vs budgeted limits for this month</p>
              </div>
              <div className="flex-1">
                {combinedData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={combinedData} margin={{ top: 10, right: 10, left: 0, bottom: 20 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#2e3650" vertical={false} />
                      <XAxis dataKey="name" stroke="#64748b" tick={{ fill: '#94a3b8', fontSize: 12 }} />
                      <YAxis stroke="#64748b" tick={{ fill: '#94a3b8', fontSize: 12 }} tickFormatter={(val) => `₹${val}`} />
                      <RechartsTooltip
                        formatter={(value: number) => formatCurrency(value)}
                        contentStyle={{ backgroundColor: '#1a1f33', borderColor: '#2e3650', borderRadius: '8px' }}
                        itemStyle={{ color: '#edf2ff' }}
                        cursor={{ fill: '#2e3650', opacity: 0.4 }}
                      />
                      <Legend verticalAlign="top" height={36} wrapperStyle={{ fontSize: '12px' }}/>
                      <Bar dataKey="Budget" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="Predicted" fill="#f59e0b" radius={[4, 4, 0, 0]}>
                        <ErrorBar dataKey="confidenceInterval" width={4} strokeWidth={2} stroke="#fcd34d" direction="y" />
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="flex h-full items-center justify-center text-sm text-muted">
                    No budget or forecast data available
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}

      {/* What-If Simulator Panel */}
      {showSimulator && <WhatIfSimulator onClose={() => setShowSimulator(false)} />}
    </div>
  );
}
