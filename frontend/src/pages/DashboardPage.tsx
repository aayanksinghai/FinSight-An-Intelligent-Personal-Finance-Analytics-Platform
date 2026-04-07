import { useQuery } from '@tanstack/react-query';
import { getTransactionSummary, getCategorySummary } from '../api/transactionApi';
import { useAuthStore } from '../store/authStore';
import { Link } from 'react-router-dom';
import { 
  PieChart, Pie, Cell, Tooltip as RechartsTooltip, ResponsiveContainer, Legend,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, ErrorBar
} from 'recharts';
import { getBudgetsForMonth } from '../api/budgetApi';
import forecastApi from '../api/forecastApi';

function formatCurrency(amount: number) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(amount);
}

export default function DashboardPage() {
  const userEmail = useAuthStore((s) => s.userEmail);

  const { data: summaryData, isLoading: summaryLoading } = useQuery({
    queryKey: ['transaction-summary'],
    queryFn: () => getTransactionSummary(),
    refetchInterval: 10000,
  });

  const currentMonth = new Date().toISOString().slice(0, 7);

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

  const income = summaryData?.find((s) => s.type === 'CREDIT')?.total ?? 0;
  const spend = summaryData?.find((s) => s.type === 'DEBIT')?.total ?? 0;
  
  // Format data for Recharts Pie
  const chartData = categoryData?.map((c) => ({
    name: c.category || 'Uncategorized',
    value: c.total,
    color: c.color || '#94A3B8'
  })) ?? [];

  // Combine Forecast and Budget Data
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
  }).filter(d => d.Budget > 0 || d.Predicted > 0).slice(0, 5); // top 5

  const isLoading = summaryLoading || categoryLoading;
  const hasData = (income > 0 || spend > 0) && chartData.length > 0;

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
          {/* KPI Cards */}
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <div className="kpi-card">
              <h3 className="kpi-label">Total Spent (30 Days)</h3>
              <p className="kpi-value text-danger">{formatCurrency(spend)}</p>
            </div>
            <div className="kpi-card">
              <h3 className="kpi-label">Total Income (30 Days)</h3>
              <p className="kpi-value text-success">{formatCurrency(income)}</p>
            </div>
            <div className="kpi-card">
              <h3 className="kpi-label">Net Flow</h3>
              <p className={`kpi-value ${income - spend >= 0 ? 'text-success' : 'text-danger'}`}>
                {income - spend >= 0 ? '+' : ''}{formatCurrency(income - spend)}
              </p>
            </div>
          </div>

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
    </div>
  );
}
