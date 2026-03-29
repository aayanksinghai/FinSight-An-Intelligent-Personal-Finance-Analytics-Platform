import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getBudgetsForMonth, upsertBudget, Budget, BudgetRequest } from '../api/budgetApi';

const CATEGORIES = [
  "Housing", "Transportation", "Food", "Utilities", 
  "Clothing", "Medical", "Insurance", "Personal", 
  "Debt", "Retirement", "Education", "Gifts", "Entertainment"
];

export default function BudgetPage() {
  const queryClient = useQueryClient();
  
  // Currently showing the budget for the current month
  const today = new Date();
  const currentMonthYear = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;
  
  const [selectedMonth, setSelectedMonth] = useState(currentMonthYear);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formCategory, setFormCategory] = useState("Food");
  const [formLimit, setFormLimit] = useState("");
  const [formError, setFormError] = useState("");

  const { data: budgets = [], isLoading } = useQuery({
    queryKey: ['budgets', selectedMonth],
    queryFn: () => getBudgetsForMonth(selectedMonth)
  });

  const mutation = useMutation({
    mutationFn: (request: BudgetRequest) => upsertBudget(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets', selectedMonth] });
      setIsModalOpen(false);
      setFormLimit("");
    },
    onError: (err: any) => {
      setFormError(err.response?.data?.message || "Failed to save budget");
    }
  });

  function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setFormError("");
    const limit = parseFloat(formLimit);
    if (isNaN(limit) || limit <= 0) {
      setFormError("Please enter a valid amount greater than 0");
      return;
    }
    mutation.mutate({
      categoryId: formCategory.toUpperCase(),
      categoryName: formCategory,
      limitAmount: limit,
      monthYear: selectedMonth
    });
  }

  return (
    <div className="flex flex-col gap-6 w-full max-w-5xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Budgets</h1>
          <p className="text-sm text-brand-purple-light mt-1">Manage your spending limits</p>
        </div>
        
        <div className="flex items-center gap-4">
          <input 
            type="month" 
            value={selectedMonth}
            onChange={(e) => setSelectedMonth(e.target.value)}
            className="input-field max-w-[150px] py-1.5"
          />
          <button 
            onClick={() => setIsModalOpen(true)}
            className="btn-primary"
          >
            + New Limit
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="animate-pulse space-y-4">
          <div className="h-24 bg-brand-dark/20 rounded-xl"></div>
          <div className="h-24 bg-brand-dark/20 rounded-xl"></div>
        </div>
      ) : budgets.length === 0 ? (
        <div className="glass-card flex flex-col items-center justify-center p-12 text-center">
          <div className="h-16 w-16 rounded-full bg-brand/10 text-brand flex items-center justify-center mb-4">
            <svg className="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3 className="text-lg font-bold text-white mb-2">No budgets set</h3>
          <p className="text-muted text-sm max-w-md">You haven't set any spending limits for this month. Create one to start tracking your expenses.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {budgets.map((b) => {
            const pct = Math.min((b.currentSpend / b.limitAmount) * 100, 100);
            const isDanger = pct >= 90;
            const isWarning = pct >= 70 && pct < 90;
            
            return (
              <div key={b.id} className="glass-card p-5 flex flex-col gap-3 relative overflow-hidden group">
                <div className="flex justify-between items-start mb-1">
                  <h3 className="font-semibold text-white">{b.categoryName}</h3>
                  <div className="text-right">
                    <span className="text-lg font-bold text-white">${b.currentSpend.toFixed(2)}</span>
                    <span className="text-xs text-muted block">of ${b.limitAmount.toFixed(2)}</span>
                  </div>
                </div>
                
                {/* Progress Bar Container */}
                <div className="h-3 w-full bg-slate-800 rounded-full overflow-hidden border border-white/5 shadow-inner">
                  <div 
                    className={`h-full rounded-full transition-all duration-1000 ease-out shadow-glow ${
                      isDanger ? 'bg-red-500 shadow-red-500/50' : 
                      isWarning ? 'bg-yellow-400 shadow-yellow-400/50' : 
                      'bg-emerald-400 shadow-emerald-400/50'
                    }`}
                    style={{ width: `${pct}%` }}
                  />
                </div>
                
                <div className="flex justify-between text-xs mt-1">
                  <span className={isDanger ? 'text-red-400 font-medium animate-pulse' : 'text-muted'}>
                    {pct.toFixed(0)}% used
                  </span>
                  <span className="text-muted">
                    ${Math.max(b.limitAmount - b.currentSpend, 0).toFixed(2)} remaining
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal Overlay */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-fade-in">
          <div className="glass-card w-full max-w-md p-6 shadow-2xl relative rounded-2xl border border-white/10 ring-1 ring-white/5">
            <h2 className="text-xl font-bold text-white mb-4">Set Envelope Limit</h2>
            
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
                  {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-[#edf2ff] ml-1">Monthly Limit ($)</label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted">$</span>
                  <input 
                    type="number" 
                    step="0.01"
                    min="1"
                    className="input-field pl-8" 
                    placeholder="500.00"
                    value={formLimit}
                    onChange={(e) => setFormLimit(e.target.value)}
                  />
                </div>
              </div>

              <div className="flex gap-3 justify-end mt-4">
                <button 
                  type="button" 
                  className="btn-secondary"
                  onClick={() => setIsModalOpen(false)}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="btn-primary"
                  disabled={mutation.isPending}
                >
                  {mutation.isPending ? 'Saving...' : 'Save Budget'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
