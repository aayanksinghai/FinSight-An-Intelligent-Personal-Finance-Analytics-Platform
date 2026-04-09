import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { changePassword, getPasswordPolicy, setPassword } from '../api/authApi';
import { deleteMyAccount, getMyProfile, getSecurityOverview, updateProfile } from '../api/profileApi';
import { useAuthStore } from '../store/authStore';
import Notice from '../components/Notice';
import { extractApiError } from '../utils/errors';
import {
  DEFAULT_PASSWORD_POLICY,
  getPasswordPolicyHint,
  isStrongPassword,
} from '../utils/validation';
import type { PasswordPolicyResponse, UserProfileResponse, UserSecurityResponse } from '../types/api';

interface ProfileForm {
  fullName: string;
  city: string;
  ageGroup: string;
  monthlyIncome: string;
}

interface PasswordForm {
  currentPassword: string;
  newPassword: string;
}

export default function ProfilePage() {
  const { clearSession } = useAuthStore();
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [security, setSecurity] = useState<UserSecurityResponse | null>(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [passwordPolicy, setPasswordPolicy] = useState<PasswordPolicyResponse>(DEFAULT_PASSWORD_POLICY);
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);

  const [profileForm, setProfileForm] = useState<ProfileForm>({
    fullName: '',
    city: '',
    ageGroup: '',
    monthlyIncome: '',
  });

  const [passwordForm, setPasswordForm] = useState<PasswordForm>({
    currentPassword: '',
    newPassword: '',
  });

  async function loadData() {
    setLoadingProfile(true);
    setError('');
    try {
      const [profileData, securityData] = await Promise.all([
        getMyProfile(),
        getSecurityOverview(),
      ]);
      setProfile(profileData);
      setSecurity(securityData);
      setProfileForm({
        fullName: profileData.fullName ?? '',
        city: profileData.city ?? '',
        ageGroup: profileData.ageGroup ?? '',
        monthlyIncome: profileData.monthlyIncome != null ? String(profileData.monthlyIncome) : '',
      });
    } catch (err) {
      setError(extractApiError(err, 'Unable to load profile.'));
    } finally {
      setLoadingProfile(false);
    }
  }

  useEffect(() => {
    void loadData();
    getPasswordPolicy()
      .then(setPasswordPolicy)
      .catch(() => { /* keep default */ });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function submitProfile(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setMessage('');
    setError('');
    setSavingProfile(true);

    try {
      const payload = {
        ...profileForm,
        monthlyIncome: profileForm.monthlyIncome === '' ? null : Number(profileForm.monthlyIncome),
      };
      const updated = await updateProfile(payload);
      setProfile(updated);
      setMessage('Profile updated successfully.');
    } catch (err) {
      setError(extractApiError(err, 'Profile update failed.'));
    } finally {
      setSavingProfile(false);
    }
  }

  async function submitPassword(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setMessage('');
    setError('');

    if (!isStrongPassword(passwordForm.newPassword, passwordPolicy)) {
      setError(getPasswordPolicyHint(passwordPolicy));
      return;
    }

    setSavingPassword(true);
    try {
      if (security?.hasPassword) {
        await changePassword(passwordForm.currentPassword, passwordForm.newPassword);
      } else {
        await setPassword(passwordForm.newPassword);
      }
      setPasswordForm({ currentPassword: '', newPassword: '' });
      setMessage('Password updated. You will be signed out...');
      setTimeout(() => {
        clearSession();
        window.location.href = '/login';
      }, 1200);
    } catch (err) {
      setError(extractApiError(err, 'Password update failed.'));
    } finally {
      setSavingPassword(false);
    }
  }

  async function handleDeleteAccount() {
    const confirmed = window.confirm(
      'Delete your account permanently? This cannot be undone.',
    );
    if (!confirmed) return;

    try {
      await deleteMyAccount();
      clearSession();
      window.location.href = '/register';
    } catch (err) {
      setError(extractApiError(err, 'Account deletion failed.'));
    }
  }

  const AGE_GROUPS = ['Under 25', '25–34', '35–44', '45–54', '55+'];

  if (loadingProfile) {
    return (
      <div className="flex items-center justify-center py-20">
        <span className="spinner" />
        <span className="ml-3 text-sm text-muted">Loading profile...</span>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-5 animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold text-[#edf2ff]">Profile</h1>
        <p className="mt-0.5 text-sm text-muted">Manage your personal details and security settings</p>
      </div>

      {message && <Notice type="success" text={message} />}
      {error && <Notice type="error" text={error} />}

      <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
        {/* ── Profile form ── */}
        <div className="glass-card p-6">
          <h2 className="mb-4 text-base font-semibold text-[#edf2ff]">Personal details</h2>
          <form onSubmit={(e) => void submitProfile(e)} id="profile-form" className="flex flex-col gap-4">
            <div className="field">
              <label htmlFor="profile-fullname" className="form-label">Full name</label>
              <input
                id="profile-fullname"
                type="text"
                className="form-input"
                value={profileForm.fullName}
                onChange={(e) => setProfileForm((p) => ({ ...p, fullName: e.target.value }))}
                placeholder="Your name"
              />
            </div>

            <div className="field">
              <label htmlFor="profile-city" className="form-label">City</label>
              <input
                id="profile-city"
                type="text"
                className="form-input"
                value={profileForm.city}
                onChange={(e) => setProfileForm((p) => ({ ...p, city: e.target.value }))}
                placeholder="e.g. Mumbai"
              />
            </div>

            <div className="field">
              <label htmlFor="profile-age-group" className="form-label">Age group</label>
              <select
                id="profile-age-group"
                className="form-input"
                value={profileForm.ageGroup}
                onChange={(e) => setProfileForm((p) => ({ ...p, ageGroup: e.target.value }))}
              >
                <option value="">Select age group</option>
                {AGE_GROUPS.map((g) => (
                  <option key={g} value={g}>{g}</option>
                ))}
              </select>
            </div>

            <div className="field">
              <label htmlFor="profile-income" className="form-label">Monthly income (₹)</label>
              <input
                id="profile-income"
                type="number"
                min="0"
                step="0.01"
                className="form-input"
                value={profileForm.monthlyIncome}
                onChange={(e) => setProfileForm((p) => ({ ...p, monthlyIncome: e.target.value }))}
                placeholder="e.g. 80000"
              />
            </div>

            <button type="submit" className="btn-primary w-full" disabled={savingProfile}>
              {savingProfile ? <><span className="spinner" /> Saving...</> : 'Save profile'}
            </button>
          </form>
        </div>

        {/* ── Security panel ── */}
        <div className="flex flex-col gap-4">
          {/* Account info */}
          <div className="glass-card p-6">
            <h2 className="mb-4 text-base font-semibold text-[#edf2ff]">Account info</h2>
            <div className="flex flex-col gap-2.5 text-sm">
              <div className="flex justify-between">
                <span className="text-muted">Email</span>
                <span className="font-medium text-[#edf2ff]">{security?.email ?? '—'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted">Member since</span>
                <span className="text-[#edf2ff]">
                  {security?.accountCreatedAt
                    ? new Date(security.accountCreatedAt).toLocaleDateString()
                    : '—'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted">Profile configured</span>
                <span className={profile?.profileConfigured ? 'text-success' : 'text-muted'}>
                  {profile?.profileConfigured ? '✓ Yes' : '✕ No'}
                </span>
              </div>
            </div>
          </div>

          {/* Change or Set password */}
          <div className="glass-card p-6">
            <h2 className="mb-4 text-base font-semibold text-[#edf2ff]">
              {security?.hasPassword ? 'Change password' : 'Set password'}
            </h2>
            <form onSubmit={(e) => void submitPassword(e)} id="password-form" className="flex flex-col gap-4">
              {security?.hasPassword && (
                <div className="field">
                  <label htmlFor="current-password" className="form-label">Current password</label>
                  <input
                    id="current-password"
                    type="password"
                    className="form-input"
                    value={passwordForm.currentPassword}
                    onChange={(e) => setPasswordForm((p) => ({ ...p, currentPassword: e.target.value }))}
                    required
                    autoComplete="current-password"
                  />
                </div>
              )}
              <div className="field">
                <label htmlFor="new-password-profile" className="form-label">New password</label>
                <input
                  id="new-password-profile"
                  type="password"
                  className="form-input"
                  value={passwordForm.newPassword}
                  onChange={(e) => setPasswordForm((p) => ({ ...p, newPassword: e.target.value }))}
                  required
                  autoComplete="new-password"
                />
                <p className="mt-1.5 text-[11px] text-muted">{getPasswordPolicyHint(passwordPolicy)}</p>
              </div>
              <button type="submit" className="btn-primary w-full" disabled={savingPassword}>
                {savingPassword ? (
                  <><span className="spinner" /> Updating...</>
                ) : (
                  security?.hasPassword ? 'Change password' : 'Set password'
                )}
              </button>
            </form>
          </div>

          {/* Danger zone */}
          <div className="glass-card border-danger/30 p-5">
            <h3 className="mb-1 text-sm font-semibold text-danger">Danger zone</h3>
            <p className="mb-3 text-xs text-muted">
              Permanently delete your account and all associated data. This cannot be undone.
            </p>
            <button
              className="btn-danger text-xs"
              onClick={() => void handleDeleteAccount()}
            >
              Delete account
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
