import { useEffect, useState } from 'react';
import { activateUser, deactivateUser, listUsers } from '../api/adminApi';
import Notice from '../components/Notice';

export default function AdminPage() {
  const [status, setStatus] = useState('all');
  const [users, setUsers] = useState([]);
  const [pageMeta, setPageMeta] = useState({ page: 0, totalPages: 1, totalElements: 0 });
  const [notice, setNotice] = useState({ type: 'info', text: '' });
  const [loading, setLoading] = useState(true);

  async function load(page = 0, selectedStatus = status) {
    setLoading(true);
    setNotice({ type: 'info', text: '' });
    try {
      const response = await listUsers(selectedStatus, page, 20);
      setUsers(response.content || []);
      setPageMeta({
        page: response.number || 0,
        totalPages: Math.max(response.totalPages || 1, 1),
        totalElements: response.totalElements || 0
      });
    } catch (requestError) {
      setNotice({ type: 'error', text: requestError?.response?.data?.message || 'Could not load users.' });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load(0, status);
  }, [status]);

  async function toggleUser(user) {
    try {
      if (user.active) {
        await deactivateUser(user.email);
        setNotice({ type: 'success', text: `${user.email} deactivated.` });
      } else {
        await activateUser(user.email);
        setNotice({ type: 'success', text: `${user.email} activated.` });
      }
      await load(pageMeta.page, status);
    } catch (requestError) {
      setNotice({ type: 'error', text: requestError?.response?.data?.message || 'Action failed.' });
    }
  }

  return (
    <section className="panel glass-card">
      <div className="panel-header">
        <h2>Admin Users</h2>
        <div className="row-actions">
          <label>
            Status
            <select value={status} onChange={(event) => setStatus(event.target.value)}>
              <option value="all">All</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </label>
          <button onClick={() => load(pageMeta.page, status)}>Refresh</button>
        </div>
      </div>

      <Notice type={notice.type} text={notice.text} />

      {loading ? <p>Loading users...</p> : null}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Email</th>
              <th>Role</th>
              <th>Active</th>
              <th>Created</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.email}>
                <td>{user.email}</td>
                <td>{user.role}</td>
                <td>{String(user.active)}</td>
                <td>{user.createdAt || '-'}</td>
                <td>
                  <button className={user.active ? 'danger-btn' : ''} onClick={() => toggleUser(user)}>
                    {user.active ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="row-actions">
        <button disabled={pageMeta.page <= 0} onClick={() => load(pageMeta.page - 1, status)}>Prev</button>
        <span>
          Page {pageMeta.page + 1} / {pageMeta.totalPages} | Users: {pageMeta.totalElements}
        </span>
        <button disabled={pageMeta.page + 1 >= pageMeta.totalPages} onClick={() => load(pageMeta.page + 1, status)}>Next</button>
      </div>
    </section>
  );
}

