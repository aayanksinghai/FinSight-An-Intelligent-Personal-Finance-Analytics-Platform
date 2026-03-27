import { useEffect, useState } from 'react';
import { changePassword } from '../api/authApi';
import { deleteMyAccount, getMyProfile, getSecurityOverview, updateProfile } from '../api/profileApi';
import { useAuthStore } from '../store/authStore';
import Notice from '../components/Notice';
import { extractApiError } from '../utils/errors';

export default function ProfilePage() {
  const { clearSession } = useAuthStore();
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState(null);
  const [security, setSecurity] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const [profileForm, setProfileForm] = useState({
    fullName: '',
    city: '',
    ageGroup: '',
    monthlyIncome: ''
  });

  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: ''
  });

  async function loadData() {
    setLoading(true);
    setError('');
    try {
      const [profileData, securityData] = await Promise.all([getMyProfile(), getSecurityOverview()]);
      setProfile(profileData);
      setSecurity(securityData);
      setProfileForm({
        fullName: profileData.fullName || '',
        city: profileData.city || '',
        ageGroup: profileData.ageGroup || '',
        monthlyIncome: profileData.monthlyIncome || ''
      });
    } catch (requestError) {
      setError(extractApiError(requestError, 'Unable to load profile.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  async function submitProfile(event) {
    event.preventDefault();
    setMessage('');
    setError('');

    try {
      const payload = {
        ...profileForm,
        monthlyIncome: profileForm.monthlyIncome === '' ? null : Number(profileForm.monthlyIncome)
      };
      const updated = await updateProfile(payload);
      setProfile(updated);
      setMessage('Profile updated successfully.');
    } catch (requestError) {
      setError(extractApiError(requestError, 'Profile update failed.'));
    }
  }

  async function submitPassword(event) {
    event.preventDefault();
    setMessage('');
    setError('');

    try {
      await changePassword(passwordForm.currentPassword, passwordForm.newPassword);
      setPasswordForm({ currentPassword: '', newPassword: '' });
      setMessage('Password changed. Sessions were revoked by backend, please login again.');
      setTimeout(() => {
        clearSession();
        window.location.href = '/login';
      }, 1200);
    } catch (requestError) {
      setError(extractApiError(requestError, 'Password change failed.'));
    }
  }

  async function handleDeleteAccount() {
    const confirmed = window.confirm('Delete account permanently? This cannot be undone.');
    if (!confirmed) return;

    try {
      await deleteMyAccount();
      clearSession();
      window.location.href = '/register';
    } catch (requestError) {
      setError(extractApiError(requestError, 'Account deletion failed.'));
    }
  }

  if (loading) {
    return <section className="panel glass-card">Loading profile...</section>;
  }

  return (
    <section className="panel-grid">
      <article className="panel glass-card">
        <h2>Profile</h2>
        <Notice type="success" text={message} />
        <Notice type="error" text={error} />

        <form className="form-grid" onSubmit={submitProfile}>
          <label>
            Full name
            <input value={profileForm.fullName} onChange={(e) => setProfileForm((p) => ({ ...p, fullName: e.target.value }))} />
          </label>
          <label>
            City
            <input value={profileForm.city} onChange={(e) => setProfileForm((p) => ({ ...p, city: e.target.value }))} />
          </label>
          <label>
            Age group
            <input value={profileForm.ageGroup} onChange={(e) => setProfileForm((p) => ({ ...p, ageGroup: e.target.value }))} />
          </label>
          <label>
            Monthly income
            <input
              type="number"
              min="0"
              step="0.01"
              value={profileForm.monthlyIncome}
              onChange={(e) => setProfileForm((p) => ({ ...p, monthlyIncome: e.target.value }))}
            />
          </label>
          <button type="submit">Save profile</button>
        </form>
      </article>

      <article className="panel glass-card">
        <h3>Security</h3>
        <p className="muted">Account: {security?.email}</p>
        <p className="muted">Created: {security?.accountCreatedAt || '-'}</p>
        <p className="muted">Last profile update: {security?.lastProfileUpdateAt || '-'}</p>
        <p className="muted">Profile configured: {String(security?.profileConfigured)}</p>

        <form className="form-grid" onSubmit={submitPassword}>
          <label>
            Current password
            <input
              type="password"
              value={passwordForm.currentPassword}
              onChange={(e) => setPasswordForm((p) => ({ ...p, currentPassword: e.target.value }))}
              required
            />
          </label>
          <label>
            New password
            <input
              type="password"
              value={passwordForm.newPassword}
              onChange={(e) => setPasswordForm((p) => ({ ...p, newPassword: e.target.value }))}
              required
            />
          </label>
          <button type="submit">Change password</button>
        </form>

        <button className="danger-btn" onClick={handleDeleteAccount}>Delete account</button>
      </article>
    </section>
  );
}

