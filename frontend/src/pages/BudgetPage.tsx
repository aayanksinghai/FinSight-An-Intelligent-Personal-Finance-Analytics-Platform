import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getBudgetsForMonth, upsertBudget, deleteBudget, Budget, BudgetRequest } from '../api/budgetApi';

// Categories must match ML model and database exactly
const CATEGORIES = [
  "Food & Dining",
  "Shopping",
  "Transportation",
  "Housing",
  "Utilities",
  "Health & Fitness",
  "Entertainment",
  "Travel",
  "Transfer",
  "Income",
];

const CATEGORY_ICONS: Record<string, string> = {
  "Food & Dining": "🍽️",
  "Shopping": "🛍️",
  "Transportation": "🚗",
  "Housing": "🏠",
  "Utilities": "💡",
  "Health & Fitness": "💪",
  "Entertainment": "🎬",
  "Travel": "✈️",
  "Transfer": "💸",
  "Income": "💰",
};

export default function BudgetPage() {
  const queryClient = useQueryClient();

  const today = new Date();
  const currentMonthYear = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;

  const [selectedMonth, setSelectedMonth] = useState(currentMonthYear);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formCategory, setFormCategory] = useState(CATEGORIES[0]);
  const [formLimit, setFormLimit] = useState('');
  const [formError, setFormError] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const { data: budgets = [], isLoading } = useQuery({
    queryKey: ['budgets', selectedMonth],
    queryFn: () => getBudgetsForMonth(selectedMonth),
  });

  const createMutation = useMutation({
    mutationFn: (request: BudgetRequest) => upsertBudget(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets', selectedMonth] });
      setIsModalOpen(false);
      setFormLimit('');
      setFormError('');
    },
    onError: (err: any) => {
      setFormError(err.response?.data?.message || 'Failed to save budget');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteBudget(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets', selectedMonth] });
      setDeletingId(null);
    },
  });

  function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setFormError('');
    const limit = parseFloat(formLimit);
    if (isNaN(limit) || limit <= 0) {
      setFormError('Please enter a valid amount greater than 0');
      return;
    }
    createMutation.mutate({
      categoryId: formCategory.toUpperCase().replace(/[^A-Z]/g, '_'),
      categoryName: formCategory,
      limitAmount: limit,
      monthYear: selectedMonth,
    });
  }

  // Summary totals
  const totalBudgeted = budgets.reduce((sum, b) => sum + b.limitAmount, 0);
  const totalSpent = budgets.reduce((sum, b) => sum + b.currentSpend, 0);
  const overallPct = totalBudgeted > 0 ? Math.min((totalSpent / totalBudgeted) * 100, 100) : 0;

  return (
    <div className="flex flex-col gap-6 w-full max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Budgets</h1>
          <p className="text-sm text-brand-purple-light mt-1">Manage your monthly spending limits</p>
        </div>
        <div className="flex items-center gap-4">
          <input
            type="month"
            value={selectedMonth}
            onChange={(e) => setSelectedMonth(e.target.value)}
            className="input-field max-w-[150px] py-1.5"
          />
          <button onClick={() => setIsModalOpen(true)} className="btn-primary">
            + New Budget
          </button>
        </div>
      </div>

      {/* Summary Card */}
      {budgets.length > 0 && (
        <div className="glass-card p-5">
          <div className="flex justify-between items-end mb-3">
            <div>
              <p className="text-sm text-muted">Total Budget</p>
              <p className="text-2xl font-bold text-white">₹{totalBudgeted.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</p>
            </div>
            <div className="text-right">
              <p className="text-sm text-muted">Total Spent</p>
              <p className={`text-2xl font-bold ${overallPct >= 90 ? 'text-red-400' : overallPct >= 70 ? 'text-yellow-400' : 'text-emerald-400'}`}>
                ₹{totalSpent.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </p>
            </div>
          </div>
          <div className="h-3 w-full bg-slate-800 rounded-full overflow-hidden border border-white/5">
            <div
              className={`h-full rounded-full transition-all duration-1000 ease-out ${
                overallPct >= 90 ? 'bg-red-500' : overallPct >= 70 ? 'bg-yellow-400' : 'bg-emerald-400'
              }`}
              style={{ width: `${overallPct}%` }}
            />
          </div>
          <p className="text-xs text-muted mt-2">{overallPct.toFixed(1)}% of total budget used across {budgets.length} categor{budgets.length !== 1 ? 'ies' : 'y'}</p>
        </div>
      )}

      {/* Budget Cards */}
      {isLoading ? (
        <div className="animate-pulse space-y-4">
          {[1, 2, 3].map(i => <div key={i} className="h-28 bg-brand-dark/20 rounded-xl" />)}
        </div>
      ) : budgets.length === 0 ? (
        <div className="glass-card flex flex-col items-center justify-center p-12 text-center">
          <div className="h-16 w-16 rounded-full bg-brand/10 text-brand flex items-center justify-center mb-4 text-3xl">💰</div>
          <h3 className="text-lg font-bold text-white mb-2">No budgets set</h3>
          <p className="text-muted text-sm max-w-md mb-6">
            Set spending limits for each category. After uploading a bank statement, your spending will be automatically tracked!
          </p>
          <button onClick={() => setIsModalOpen(true)} className="btn-primary">
            Create Your First Budget
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {budgets.map((b) => {
            const pct = totalBudgeted > 0 ? Math.min((b.currentSpend / b.limitAmount) * 100, 100) : 0;
            const isDanger = pct >= 90;
            const isWarning = pct >= 70 && pct < 90;
            const remaining = Math.max(b.limitAmount - b.currentSpend, 0);
            const icon = CATEGORY_ICONS[b.categoryName] || '📊';

            return (
              <div key={b.id} className="glass-card p-5 flex flex-col gap-3 relative overflow-hidden group">
                {/* Delete Button */}
                <button
                  onClick={() => setDeletingId(b.id)}
                  className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity text-muted hover:text-red-400 text-xs"
                  title="Delete budget"
                >
                  ✕
                </button>

                <div className="flex items-center gap-2 mb-1">
                  <span className="text-2xl">{icon}</span>
                  <h3 className="font-semibold text-white flex-1">{b.categoryName}</h3>
                </div>

                <div className="flex justify-between text-sm">
                  <span className="text-muted">Spent</span>
                  <span className={`font-bold ${isDanger ? 'text-red-400' : isWarning ? 'text-yellow-400' : 'text-white'}`}>
                    ₹{b.currentSpend.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </span>
                </div>

                {/* Progress Bar */}
                <div className="h-2.5 w-full bg-slate-800 rounded-full overflow-hidden border border-white/5 shadow-inner">
                  <div
                    className={`h-full rounded-full transition-all duration-1000 ease-out ${
                      isDanger ? 'bg-red-500' : isWarning ? 'bg-yellow-400' : 'bg-emerald-400'
                    }`}
                    style={{ width: `${pct}%` }}
                  />
                </div>

                <div className="flex justify-between text-xs">
                  <span className={isDanger ? 'text-red-400 font-medium animate-pulse' : 'text-muted'}>
                    {pct.toFixed(0)}% used
                  </span>
                  <span className="text-muted">
                    ₹{remaining.toLocaleString('en-IN', { minimumFractionDigits: 2 })} of ₹{b.limitAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 })} left
                  </span>
                </div>

                {isDanger && (
                  <div className="text-xs text-red-400 bg-red-500/10 rounded-lg px-3 py-1.5 border border-red-500/20 font-medium">
                    ⚠️ Budget {pct >= 100 ? 'exceeded' : 'almost exceeded'}!
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deletingId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="glass-card w-full max-w-sm p-6 shadow-2xl rounded-2xl border border-white/10">
            <h2 className="text-lg font-bold text-white mb-2">Delete Budget?</h2>
            <p className="text-sm text-muted mb-6">This will remove the spending limit. Actual transactions are not affected.</p>
            <div className="flex gap-3 justify-end">
              <button className="btn-secondary" onClick={() => setDeletingId(null)}>Cancel</button>
              <button
                className="px-4 py-2 rounded-lg bg-red-500 hover:bg-red-600 text-white text-sm font-medium transition-colors"
                onClick={() => deleteMutation.mutate(deletingId)}
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create Budget Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="glass-card w-full max-w-md p-6 shadow-2xl relative rounded-2xl border border-white/10 ring-1 ring-white/5">
            <h2 className="text-xl font-bold text-white mb-1">Set Spending Limit</h2>
            <p className="text-sm text-muted mb-5">Your spending will be tracked automatically when you upload statements.</p>

            {formError && (
              <div className="mb-4 p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-sm">
                {formError}
              </div>
            )}

            <form onSubmit={handleSave} className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-[#edf2ff] ml-1">Category</label>
                <select
                  className="input-field"
                  value={formCategory}
                  onChange={(e) => setFormCategory(e.target.value)}
                >
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {CATEGORY_ICONS[c]} {c}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-[#edf2ff] ml-1">Monthly Limit (₹)</label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted">₹</span>
                  <input
                    type="number"
                    step="0.01"
                    min="1"
                    className="input-field pl-8"
                    placeholder="5000.00"
                    value={formLimit}
                    onChange={(e) => setFormLimit(e.target.value)}
                  />
                </div>
              </div>

              <div className="flex gap-3 justify-end mt-2">
                <button type="button" className="btn-secondary" onClick={() => { setIsModalOpen(false); setFormError(''); }}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={createMutation.isPending}>
                  {createMutation.isPending ? 'Saving...' : 'Save Budget'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
