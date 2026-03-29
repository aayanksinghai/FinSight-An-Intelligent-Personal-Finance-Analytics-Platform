import type { ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { logout } from '../api/authApi';

// ─── Nav icons (inline SVGs — no icon package needed) ───────────────────────

function IconDashboard() {
  return (
    <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
        d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
    </svg>
  );
}

function IconUpload() {
  return (
    <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
        d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
    </svg>
  );
}

function IconProfile() {
  return (
    <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
        d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
    </svg>
  );
}

function IconAdmin() {
  return (
    <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
        d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
    </svg>
  );
}

function IconLogout() {
  return (
    <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
        d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
    </svg>
  );
}

function IconBudget() {
  return (
    <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
        d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  );
}

// ─── Layout ─────────────────────────────────────────────────────────────────

interface LayoutProps {
  children: ReactNode;
}

export default function Layout({ children }: LayoutProps) {
  const navigate = useNavigate();
  const { role, userEmail, clearSession } = useAuthStore();

  async function handleSignOut() {
    try {
      await logout();
    } catch {
      // Ignore API errors — clear local auth state regardless.
    } finally {
      clearSession();
      navigate('/login');
    }
  }

  const initials = userEmail
    ? userEmail.slice(0, 2).toUpperCase()
    : '??';

  return (
    <div className="flex min-h-screen gap-0">
      {/* ── Sidebar ── */}
      <aside className="sticky top-0 flex h-screen w-60 flex-shrink-0 flex-col gap-3 p-3">
        <div className="glass-card flex flex-1 flex-col gap-3 p-4">
          {/* Logo */}
          <div className="mb-1 flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-brand to-brand-purple text-sm font-bold text-white shadow-glow">
              FS
            </div>
            <div>
              <p className="text-sm font-semibold text-[#edf2ff]">FinSight</p>
              <p className="text-[10px] text-muted">Finance Analytics</p>
            </div>
          </div>

          <hr className="border-stroke" />

          {/* Navigation */}
          <nav className="flex flex-col gap-1">
            <NavLink
              to="/"
              end
              className={({ isActive }) =>
                `nav-link ${isActive ? 'active' : ''}`
              }
            >
              <IconDashboard />
              Dashboard
            </NavLink>

            <NavLink
              to="/budgets"
              className={({ isActive }) =>
                `nav-link ${isActive ? 'active' : ''}`
              }
            >
              <IconBudget />
              Budgets
            </NavLink>

            <NavLink
              to="/upload"
              className={({ isActive }) =>
                `nav-link ${isActive ? 'active' : ''}`
              }
            >
              <IconUpload />
              Upload Statement
            </NavLink>

            <NavLink
              to="/profile"
              className={({ isActive }) =>
                `nav-link ${isActive ? 'active' : ''}`
              }
            >
              <IconProfile />
              Profile
            </NavLink>

            {role === 'ADMIN' && (
              <NavLink
                to="/admin"
                className={({ isActive }) =>
                  `nav-link ${isActive ? 'active' : ''}`
                }
              >
                <IconAdmin />
                Admin
              </NavLink>
            )}
          </nav>

          {/* Spacer */}
          <div className="flex-1" />

          {/* User chip */}
          <div className="rounded-xl border border-stroke bg-white/[0.03] p-3">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg bg-brand/20 text-xs font-bold text-brand">
                {initials}
              </div>
              <div className="min-w-0">
                <p className="truncate text-xs font-medium text-[#edf2ff]">{userEmail}</p>
                <p className="text-[10px] text-muted">{role ?? 'USER'}</p>
              </div>
            </div>
          </div>

          {/* Sign out */}
          <button
            className="btn-danger w-full text-xs"
            onClick={() => void handleSignOut()}
          >
            <IconLogout />
            Sign out
          </button>
        </div>
      </aside>

      {/* ── Main content ── */}
      <main className="min-w-0 flex-1 p-4 animate-fade-in">
        {children}
      </main>
    </div>
  );
}
