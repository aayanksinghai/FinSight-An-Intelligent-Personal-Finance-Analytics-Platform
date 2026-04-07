import { useEffect, useState } from 'react';
import { activateUser, deactivateUser, listUsers } from '../api/adminApi';
import forecastApi, { AccuracyResponse } from '../api/forecastApi';
import Notice from '../components/Notice';
import type { AdminUserResponse } from '../types/api';

interface NoticeState {
  type: 'success' | 'error' | 'info';
  text: string;
}

interface PageMeta {
  page: number;
  totalPages: number;
  totalElements: number;
}

export default function AdminPage() {
  const [status, setStatus] = useState<'all' | 'active' | 'inactive'>('all');
  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [pageMeta, setPageMeta] = useState<PageMeta>({
    page: 0,
    totalPages: 1,
    totalElements: 0,
  });
  const [notice, setNotice] = useState<NoticeState>({ type: 'info', text: '' });
  const [loading, setLoading] = useState(true);
  const [accuracy, setAccuracy] = useState<AccuracyResponse | null>(null);

  useEffect(() => {
    forecastApi.getAccuracy().then(setAccuracy).catch(console.error);
  }, []);

  async function load(page = 0, selectedStatus: typeof status = status) {
    setLoading(true);
    setNotice({ type: 'info', text: '' });
    try {
      const response = await listUsers(selectedStatus, page, 20);
      setUsers(response.content ?? []);
      setPageMeta({
        page: response.number ?? 0,
        totalPages: Math.max(response.totalPages ?? 1, 1),
        totalElements: response.totalElements ?? 0,
      });
    } catch (err) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setNotice({ type: 'error', text: msg ?? 'Could not load users.' });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load(0, status);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status]);

  async function toggleUser(user: AdminUserResponse) {
    try {
      if (user.active) {
        await deactivateUser(user.email);
        setNotice({ type: 'success', text: `${user.email} deactivated.` });
      } else {
        await activateUser(user.email);
        setNotice({ type: 'success', text: `${user.email} activated.` });
      }
      await load(pageMeta.page, status);
    } catch (err) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setNotice({ type: 'error', text: msg ?? 'Action failed.' });
    }
  }

  return (
    <div className="flex flex-col gap-5 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#edf2ff]">Admin — Users</h1>
          <p className="mt-0.5 text-sm text-muted">
            {pageMeta.totalElements} total users
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="field flex-row items-center gap-2">
            <label htmlFor="admin-status-filter" className="form-label whitespace-nowrap">
              Status
            </label>
            <select
              id="admin-status-filter"
              className="form-input w-auto"
              value={status}
              onChange={(e) => setStatus(e.target.value as typeof status)}
            >
              <option value="all">All</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </div>
          <button
            className="btn-ghost"
            onClick={() => void load(pageMeta.page, status)}
            disabled={loading}
          >
            {loading ? <span className="spinner" /> : '↺'} Refresh
          </button>
        </div>
      </div>

      {accuracy && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <div className="kpi-card bg-brand/10 border border-brand/30">
            <h3 className="kpi-label">AI Global Accuracy</h3>
            <p className="kpi-value text-success">{accuracy.accuracyPercentage}%</p>
          </div>
          <div className="kpi-card bg-brand/10 border border-brand/30">
            <h3 className="kpi-label">Global MAPE</h3>
            <p className="kpi-value text-warning">{accuracy.mape}%</p>
          </div>
          <div className="kpi-card bg-brand/10 border border-brand/30">
            <h3 className="kpi-label">Total Predictions Tracked</h3>
            <p className="kpi-value text-info">{accuracy.totalSnapshots}</p>
          </div>
        </div>
      )}


      {notice.text && <Notice type={notice.type} text={notice.text} />}

      {/* Table */}
      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th>Member since</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {loading && users.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-muted">
                    <span className="spinner mx-auto block" />
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-sm text-muted">
                    No users found.
                  </td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr key={user.email}>
                    <td className="font-medium">{user.email}</td>
                    <td>
                      <span className={user.role === 'ADMIN' ? 'badge badge-red' : 'badge badge-blue'}>
                        {user.role}
                      </span>
                    </td>
                    <td>
                      <span className={user.active ? 'badge badge-green' : 'badge badge-red'}>
                        {user.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="text-muted text-xs">
                      {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td>
                      <button
                        className={user.active ? 'btn-danger text-xs px-3 py-1.5' : 'btn-ghost text-xs px-3 py-1.5'}
                        onClick={() => void toggleUser(user)}
                      >
                        {user.active ? 'Deactivate' : 'Activate'}
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between border-t border-stroke px-4 py-3">
          <span className="text-xs text-muted">
            Page {pageMeta.page + 1} of {pageMeta.totalPages}
          </span>
          <div className="flex gap-2">
            <button
              className="btn-ghost text-xs px-3 py-1.5"
              disabled={pageMeta.page <= 0}
              onClick={() => void load(pageMeta.page - 1, status)}
            >
              ← Prev
            </button>
            <button
              className="btn-ghost text-xs px-3 py-1.5"
              disabled={pageMeta.page + 1 >= pageMeta.totalPages}
              onClick={() => void load(pageMeta.page + 1, status)}
            >
              Next →
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
