import { useState, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getTransactions, TransactionFilters } from '../api/transactionApi';
import type { TransactionResponse } from '../types/transaction';

const CATEGORIES = [
  'Food & Dining', 'Shopping', 'Transportation', 'Housing',
  'Utilities', 'Health & Fitness', 'Entertainment', 'Travel',
  'Transfer', 'Income', 'Uncategorized',
];

const CATEGORY_COLORS: Record<string, string> = {
  'Food & Dining':    '#f97316',
  'Shopping':         '#a855f7',
  'Transportation':   '#3b82f6',
  'Housing':          '#06b6d4',
  'Utilities':        '#84cc16',
  'Health & Fitness': '#ec4899',
  'Entertainment':    '#f59e0b',
  'Travel':           '#14b8a6',
  'Transfer':         '#6366f1',
  'Income':           '#22c55e',
  'Uncategorized':    '#64748b',
};

function formatINR(amount: number) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', maximumFractionDigits: 2,
  }).format(amount);
}

function formatDate(isoStr: string) {
  return new Date(isoStr).toLocaleDateString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
  });
}

function CategoryBadge({ name }: { name?: string }) {
  const label = name || 'Uncategorized';
  const color = CATEGORY_COLORS[label] ?? '#64748b';
  return (
    <span
      className="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
      style={{ backgroundColor: `${color}22`, color, border: `1px solid ${color}44` }}
    >
      {label}
    </span>
  );
}

function TypeBadge({ type }: { type: string }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-bold ${
      type === 'CREDIT'
        ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
        : 'bg-red-500/10 text-red-400 border border-red-500/20'
    }`}>
      {type === 'CREDIT' ? '↑' : '↓'} {type}
    </span>
  );
}

export default function TransactionsPage() {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<TransactionFilters>({});
  const [searchInput, setSearchInput] = useState('');

  // Debounce search: only fire query after user stops typing
  const applySearch = useCallback(() => {
    setFilters(f => ({ ...f, search: searchInput || undefined }));
    setPage(0);
  }, [searchInput]);

  const { data, isLoading } = useQuery({
    queryKey: ['transactions', page, filters],
    queryFn: () => getTransactions(page, 20, filters),
    placeholderData: (prev) => prev,
  });

  const transactions: TransactionResponse[] = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;
  const totalElements = data?.totalElements ?? 0;

  function setFilter(key: keyof TransactionFilters, value: string) {
    setFilters(f => ({ ...f, [key]: value || undefined }));
    setPage(0);
  }

  function clearFilters() {
    setFilters({});
    setSearchInput('');
    setPage(0);
  }

  const hasFilters = Object.keys(filters).length > 0;

  return (
    <div className="flex flex-col gap-5 animate-fade-in">
      {/* Header */}
      <div className="flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#edf2ff]">Transactions</h1>
          <p className="mt-0.5 text-sm text-muted">
            {totalElements > 0 ? `${totalElements.toLocaleString()} transactions found` : 'Browse and filter your transactions'}
          </p>
        </div>
        {hasFilters && (
          <button onClick={clearFilters} className="btn-ghost text-xs">
            ✕ Clear filters
          </button>
        )}
      </div>

      {/* Filter Bar */}
      <div className="glass-card p-4 flex flex-wrap gap-3 items-center">
        {/* Search */}
        <div className="relative flex-1 min-w-[200px]">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted text-sm">🔍</span>
          <input
            type="text"
            className="input-field pl-9 w-full text-sm py-2"
            placeholder="Search merchant or description…"
            value={searchInput}
            onChange={e => setSearchInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && applySearch()}
            onBlur={applySearch}
          />
        </div>

        {/* Type filter */}
        <select
          className="input-field text-sm py-2 min-w-[130px]"
          value={filters.type ?? ''}
          onChange={e => setFilter('type', e.target.value)}
        >
          <option value="">All Types</option>
          <option value="DEBIT">↓ Debit</option>
          <option value="CREDIT">↑ Credit</option>
        </select>

        {/* Category filter */}
        <select
          className="input-field text-sm py-2 min-w-[170px]"
          value={filters.category ?? ''}
          onChange={e => setFilter('category', e.target.value)}
        >
          <option value="">All Categories</option>
          {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
        </select>

        {/* Date range */}
        <input
          type="date"
          className="input-field text-sm py-2"
          value={filters.from ?? ''}
          onChange={e => setFilter('from', e.target.value ? new Date(e.target.value).toISOString() : '')}
          title="From date"
        />
        <span className="text-muted text-sm">→</span>
        <input
          type="date"
          className="input-field text-sm py-2"
          value={filters.to ? filters.to.split('T')[0] : ''}
          onChange={e => setFilter('to', e.target.value ? new Date(e.target.value + 'T23:59:59').toISOString() : '')}
          title="To date"
        />
      </div>

      {/* Transactions Table */}
      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="data-table w-full">
            <thead>
              <tr>
                <th className="text-left">Date</th>
                <th className="text-left">Merchant / Description</th>
                <th className="text-left">Category</th>
                <th className="text-center">Type</th>
                <th className="text-right">Amount</th>
              </tr>
            </thead>
            <tbody>
              {isLoading && transactions.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center">
                    <span className="spinner mx-auto block" />
                  </td>
                </tr>
              ) : transactions.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center">
                    <div className="flex flex-col items-center gap-3">
                      <span className="text-3xl">📭</span>
                      <p className="text-sm font-semibold text-[#edf2ff]">No transactions found</p>
                      {hasFilters ? (
                        <p className="text-xs text-muted">Try adjusting or clearing your filters.</p>
                      ) : (
                        <Link to="/upload" className="btn-primary text-xs no-underline">
                          ↑ Upload a Bank Statement
                        </Link>
                      )}
                    </div>
                  </td>
                </tr>
              ) : (
                transactions.map((txn) => (
                  <tr key={txn.id} className={`transition-colors hover:bg-white/[0.02] ${isLoading ? 'opacity-50' : ''}`}>
                    <td className="text-xs text-muted whitespace-nowrap">
                      {txn.occurredAt ? formatDate(txn.occurredAt) : '—'}
                    </td>
                    <td>
                      <p className="font-medium text-[#edf2ff] text-sm truncate max-w-[240px]">
                        {txn.merchant || txn.description}
                      </p>
                      {txn.merchant && txn.description !== txn.merchant && (
                        <p className="text-[11px] text-muted truncate max-w-[240px]">{txn.description}</p>
                      )}
                    </td>
                    <td><CategoryBadge name={txn.categoryName ?? undefined} /></td>
                    <td className="text-center"><TypeBadge type={txn.type} /></td>
                    <td className={`text-right font-mono font-semibold text-sm ${
                      txn.type === 'CREDIT' ? 'text-emerald-400' : 'text-red-400'
                    }`}>
                      {txn.type === 'CREDIT' ? '+' : '-'}{formatINR(txn.amount)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-stroke px-5 py-3">
            <span className="text-xs text-muted">Page {page + 1} of {totalPages}</span>
            <div className="flex gap-2">
              <button
                className="btn-ghost text-xs px-3 py-1.5"
                disabled={page <= 0}
                onClick={() => setPage(p => p - 1)}
              >← Prev</button>
              <button
                className="btn-ghost text-xs px-3 py-1.5"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage(p => p + 1)}
              >Next →</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
