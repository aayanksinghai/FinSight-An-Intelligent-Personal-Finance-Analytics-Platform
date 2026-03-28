import { Link } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

// ─── Placeholder stat cards for features coming in future phases ─────────────

interface StatCard {
  label: string;
  value: string;
  change?: string;
  trend?: 'up' | 'down' | 'neutral';
  color?: string;
}

const COMING_SOON_STATS: StatCard[] = [
  { label: 'Total Spend (MTD)', value: '₹ —', change: 'Upload a statement to begin', trend: 'neutral', color: 'text-brand' },
  { label: 'Total Income (MTD)', value: '₹ —', change: 'Phase 2 — ingestion', trend: 'neutral', color: 'text-success' },
  { label: 'Net Savings', value: '₹ —', change: 'Phase 3 — dashboard', trend: 'neutral', color: 'text-brand-purple' },
  { label: 'Stress Score', value: '—', change: 'Phase 7 — ML scoring', trend: 'neutral', color: 'text-warning' },
];

interface FeatureStatus {
  name: string;
  status: 'done' | 'next' | 'upcoming';
  phase: string;
}

const FEATURES: FeatureStatus[] = [
  { name: 'User registration & authentication', status: 'done', phase: 'Phase 1' },
  { name: 'Profile management & admin panel', status: 'done', phase: 'Phase 1' },
  { name: 'TypeScript + Tailwind migration', status: 'done', phase: 'Phase 1.1' },
  { name: 'Bank statement ingestion (CSV/XLS/PDF)', status: 'next', phase: 'Phase 2' },
  { name: 'Transaction CRUD + deduplication', status: 'upcoming', phase: 'Phase 3' },
  { name: 'Budget management + alerts', status: 'upcoming', phase: 'Phase 4' },
  { name: 'ML categorization service', status: 'upcoming', phase: 'Phase 5' },
  { name: 'Anomaly detection (per-user LSTM)', status: 'upcoming', phase: 'Phase 6' },
  { name: 'Spend forecasting + stress score', status: 'upcoming', phase: 'Phase 7' },
  { name: 'What-if simulator', status: 'upcoming', phase: 'Phase 8' },
  { name: 'AI chat assistant', status: 'upcoming', phase: 'Phase 9' },
];

const statusConfig = {
  done: { label: 'Done', cls: 'badge-green' },
  next: { label: 'Next', cls: 'badge-blue' },
  upcoming: { label: 'Upcoming', cls: 'badge-yellow' },
};

function TrendIcon({ trend }: { trend: StatCard['trend'] }) {
  if (trend === 'up') return <span className="text-success">↑</span>;
  if (trend === 'down') return <span className="text-danger">↓</span>;
  return <span className="text-muted">–</span>;
}

export default function DashboardPage() {
  const { userEmail, role } = useAuthStore();

  return (
    <div className="flex flex-col gap-5 animate-fade-in">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-[#edf2ff]">Dashboard</h1>
        <p className="mt-0.5 text-sm text-muted">
          Welcome back, <span className="text-brand">{userEmail ?? 'user'}</span>
          {role === 'ADMIN' && (
            <span className="ml-2 badge badge-red">ADMIN</span>
          )}
        </p>
      </div>

      {/* KPI row */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {COMING_SOON_STATS.map((stat) => (
          <div key={stat.label} className="kpi-card">
            <p className="text-xs text-muted">{stat.label}</p>
            <p className={`mt-1 text-2xl font-bold ${stat.color ?? 'text-[#edf2ff]'}`}>
              {stat.value}
            </p>
            <p className="mt-1 flex items-center gap-1 text-[11px] text-muted">
              <TrendIcon trend={stat.trend} />
              {stat.change}
            </p>
          </div>
        ))}
      </div>

      {/* Charts placeholder — Phase 3 */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="glass-card flex flex-col items-center justify-center gap-4 p-8 md:col-span-2">
          <div className="flex h-12 w-12 items-center justify-center rounded-full border border-brand/30 bg-brand/10 text-2xl">
            📊
          </div>
          <p className="text-sm font-semibold text-[#edf2ff]">Your charts will appear here after you upload a statement</p>
          <p className="max-w-sm text-center text-xs text-muted">
            Monthly spend breakdown, category pie chart, 12-month trend, and top merchants —
            all powered by Recharts and your real transaction data (Phase 3).
          </p>
          <Link to="/upload" className="btn-primary no-underline hover:no-underline">
            ↑ Upload Your First Statement
          </Link>
        </div>
      </div>

      {/* Build progress */}
      <div className="glass-card p-5">
        <h2 className="mb-4 text-base font-semibold text-[#edf2ff]">Platform Build Progress</h2>
        <div className="flex flex-col gap-2">
          {FEATURES.map((feat) => {
            const cfg = statusConfig[feat.status];
            return (
              <div
                key={feat.name}
                className="flex items-center justify-between rounded-xl px-3 py-2.5 transition-colors hover:bg-white/[0.025]"
              >
                <div className="flex items-center gap-3">
                  <span className={`badge ${cfg.cls} w-16 justify-center`}>{cfg.label}</span>
                  <span className="text-sm text-[#edf2ff]">{feat.name}</span>
                </div>
                <span className="text-[11px] text-muted">{feat.phase}</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
