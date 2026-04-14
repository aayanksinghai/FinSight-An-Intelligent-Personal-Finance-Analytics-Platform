import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import stressScoreApi, { SimulateAdjustment } from '../api/stressScoreApi';
import { getCategorySummary } from '../api/transactionApi';

function formatCurrency(n: number) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);
}

export default function WhatIfSimulator({ onClose }: { onClose: () => void }) {
  const currentMonth = '2025-02';

  const { data: categories } = useQuery({
    queryKey: ['category-summary'],
    queryFn: getCategorySummary,
  });

  // Adjustments state
  const [reductions, setReductions] = useState<Record<string, number>>({}); // category → pct
  const [newRecurring, setNewRecurring] = useState<{ label: string; amount: number }[]>([]);
  const [newRecLabel, setNewRecLabel] = useState('');
  const [newRecAmount, setNewRecAmount] = useState('');

  const simulate = useMutation({
    mutationFn: () => {
      const adjustments: SimulateAdjustment[] = [];

      Object.entries(reductions).forEach(([cat, pct]) => {
        if (pct > 0) {
          adjustments.push({ type: 'reduce_category', category: cat, pct });
        }
      });

      newRecurring.forEach((item) => {
        adjustments.push({ type: 'add_recurring', label: item.label, amount: item.amount });
      });

      return stressScoreApi.simulate({ monthYear: currentMonth, adjustments });
    },
  });

  const addRecurring = () => {
    const amt = parseFloat(newRecAmount);
    if (newRecLabel.trim() && !isNaN(amt) && amt > 0) {
      setNewRecurring((prev) => [...prev, { label: newRecLabel.trim(), amount: amt }]);
      setNewRecLabel('');
      setNewRecAmount('');
    }
  };

  const result = simulate.data;

  const scoreColor = (score: number) => {
    if (score < 30) return '#22c55e';
    if (score < 55) return '#f59e0b';
    if (score < 75) return '#f97316';
    return '#ef4444';
  };

  return (
    <div
      style={{
        position: 'fixed', inset: 0, zIndex: 50,
        background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)',
        display: 'flex', alignItems: 'center', justifyContent: 'flex-end',
      }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div
        style={{
          width: '100%', maxWidth: 480, height: '100vh',
          background: 'linear-gradient(135deg, #13172d 0%, #1a1f38 100%)',
          borderLeft: '1px solid rgba(99,102,241,0.2)',
          display: 'flex', flexDirection: 'column',
          boxShadow: '-20px 0 60px rgba(0,0,0,0.5)',
        }}
      >
        {/* Header */}
        <div style={{ padding: '24px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h2 style={{ margin: 0, fontSize: 18, fontWeight: 700, color: '#edf2ff' }}>
                🧮 What-If Simulator
              </h2>
              <p style={{ margin: '4px 0 0', fontSize: 12, color: '#64748b' }}>
                Project changes to your stress score — no data is modified
              </p>
            </div>
            <button
              onClick={onClose}
              style={{
                background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)',
                borderRadius: 8, color: '#94a3b8', cursor: 'pointer',
                padding: '6px 12px', fontSize: 13,
              }}
            >
              ✕
            </button>
          </div>
        </div>

        {/* Content */}
        <div style={{ flex: 1, overflowY: 'auto', padding: 24 }}>
          {/* Section: Reduce Category Spending */}
          <div style={{ marginBottom: 28 }}>
            <h3 style={{ margin: '0 0 12px', fontSize: 13, fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Reduce Category Spending
            </h3>
            {categories && categories.length > 0 ? (
              categories.slice(0, 8).map((cat) => {
                const pct = reductions[cat.category || 'Uncategorized'] ?? 0;
                const key = cat.category || 'Uncategorized';
                return (
                  <div key={key} style={{ marginBottom: 14 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                      <span style={{ fontSize: 13, color: '#edf2ff' }}>{key}</span>
                      <span style={{ fontSize: 13, color: pct > 0 ? '#22c55e' : '#64748b', fontWeight: 600 }}>
                        {pct > 0 ? `-${pct}%` : '0%'}
                        {pct > 0 && <span style={{ color: '#64748b', fontWeight: 400, marginLeft: 4 }}>
                          ({formatCurrency(cat.total * pct / 100)} saved)
                        </span>}
                      </span>
                    </div>
                    <input
                      type="range" min={0} max={100} step={5}
                      value={pct}
                      onChange={(e) => setReductions((prev) => ({ ...prev, [key]: Number(e.target.value) }))}
                      style={{ width: '100%', accentColor: '#6366f1', cursor: 'pointer' }}
                    />
                  </div>
                );
              })
            ) : (
              <p style={{ color: '#64748b', fontSize: 13 }}>No category data available. Upload a statement first.</p>
            )}
          </div>

          {/* Section: Add Recurring Expense */}
          <div style={{ marginBottom: 28 }}>
            <h3 style={{ margin: '0 0 12px', fontSize: 13, fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Add New Recurring Expense
            </h3>
            <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
              <input
                placeholder="Label (e.g. Netflix)" value={newRecLabel}
                onChange={(e) => setNewRecLabel(e.target.value)}
                style={{
                  flex: 1, background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)',
                  borderRadius: 8, color: '#edf2ff', padding: '8px 12px', fontSize: 13,
                }}
              />
              <input
                placeholder="₹ Amount" value={newRecAmount}
                onChange={(e) => setNewRecAmount(e.target.value)}
                type="number" min={0}
                style={{
                  width: 100, background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)',
                  borderRadius: 8, color: '#edf2ff', padding: '8px 12px', fontSize: 13,
                }}
              />
              <button
                onClick={addRecurring}
                style={{
                  background: 'rgba(99,102,241,0.15)', border: '1px solid rgba(99,102,241,0.3)',
                  borderRadius: 8, color: '#818cf8', cursor: 'pointer', padding: '8px 14px', fontSize: 13,
                }}
              >+</button>
            </div>
            {newRecurring.map((item, i) => (
              <div key={i} style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                background: 'rgba(99,102,241,0.08)', borderRadius: 8, padding: '8px 12px', marginBottom: 6,
              }}>
                <span style={{ fontSize: 13, color: '#edf2ff' }}>{item.label}</span>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span style={{ fontSize: 13, color: '#f97316' }}>+{formatCurrency(item.amount)}</span>
                  <button
                    onClick={() => setNewRecurring((prev) => prev.filter((_, j) => j !== i))}
                    style={{ background: 'none', border: 'none', color: '#64748b', cursor: 'pointer', fontSize: 14 }}
                  >✕</button>
                </div>
              </div>
            ))}
          </div>

          {/* Run Simulation Button */}
          <button
            onClick={() => simulate.mutate()}
            disabled={simulate.isPending}
            style={{
              width: '100%', padding: '12px 20px',
              background: 'linear-gradient(135deg, #6366f1, #818cf8)',
              border: 'none', borderRadius: 10, color: '#fff',
              fontSize: 14, fontWeight: 700, cursor: 'pointer',
              opacity: simulate.isPending ? 0.7 : 1,
              marginBottom: 24,
            }}
          >
            {simulate.isPending ? '⏳ Running...' : '▶ Run Simulation'}
          </button>

          {/* Error */}
          {simulate.isError && (
            <div style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.2)', borderRadius: 10, padding: 14, marginBottom: 16 }}>
              <p style={{ margin: 0, fontSize: 13, color: '#f87171' }}>Simulation failed. Please try again.</p>
            </div>
          )}

          {/* Results */}
          {result && (
            <div style={{ background: 'rgba(99,102,241,0.06)', border: '1px solid rgba(99,102,241,0.2)', borderRadius: 12, padding: 20 }}>
              <h3 style={{ margin: '0 0 16px', fontSize: 14, fontWeight: 700, color: '#edf2ff' }}>📊 Projected Impact</h3>

              {/* Score Comparison */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 16 }}>
                <div style={{ textAlign: 'center', padding: 14, background: 'rgba(255,255,255,0.04)', borderRadius: 10 }}>
                  <p style={{ margin: '0 0 4px', fontSize: 11, color: '#64748b', textTransform: 'uppercase' }}>Current Score</p>
                  <p style={{ margin: 0, fontSize: 28, fontWeight: 800, color: scoreColor(result.baseScore) }}>{result.baseScore}</p>
                </div>
                <div style={{ textAlign: 'center', padding: 14, background: 'rgba(255,255,255,0.04)', borderRadius: 10 }}>
                  <p style={{ margin: '0 0 4px', fontSize: 11, color: '#64748b', textTransform: 'uppercase' }}>Projected Score</p>
                  <p style={{ margin: 0, fontSize: 28, fontWeight: 800, color: scoreColor(result.projectedScore) }}>{result.projectedScore}</p>
                </div>
              </div>

              {/* Delta indicators */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div style={{ padding: 12, background: 'rgba(255,255,255,0.04)', borderRadius: 10 }}>
                  <p style={{ margin: '0 0 4px', fontSize: 11, color: '#64748b' }}>Score Change</p>
                  <p style={{
                    margin: 0, fontSize: 18, fontWeight: 700,
                    color: result.scoreDelta <= 0 ? '#22c55e' : '#ef4444',
                  }}>
                    {result.scoreDelta > 0 ? '+' : ''}{result.scoreDelta}
                  </p>
                </div>
                <div style={{ padding: 12, background: 'rgba(255,255,255,0.04)', borderRadius: 10 }}>
                  <p style={{ margin: '0 0 4px', fontSize: 11, color: '#64748b' }}>Balance Change</p>
                  <p style={{
                    margin: 0, fontSize: 18, fontWeight: 700,
                    color: result.balanceDelta >= 0 ? '#22c55e' : '#ef4444',
                  }}>
                    {result.balanceDelta >= 0 ? '+' : ''}{formatCurrency(result.balanceDelta)}
                  </p>
                </div>
              </div>

              <p style={{ margin: '14px 0 0', fontSize: 12, color: '#64748b', lineHeight: 1.5 }}>
                {result.adjustmentSummary}
              </p>

              <div style={{ marginTop: 12, padding: '8px 12px', background: 'rgba(255,255,255,0.03)', borderRadius: 8, fontSize: 11, color: '#475569' }}>
                ⚠️ This is a read-only projection. No actual data was changed.
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
