import { create } from 'zustand';
import { clearStoredTokens, getStoredTokens, setStoredTokens } from './tokenStore';
import { decodeJwtPayload } from '../utils/jwt';

function buildAuthState(accessToken, refreshToken) {
  const payload = decodeJwtPayload(accessToken);
  return {
    accessToken,
    refreshToken,
    userEmail: payload?.sub || null,
    role: payload?.role || null,
    isAuthenticated: Boolean(accessToken && refreshToken)
  };
}

const initialTokens = getStoredTokens();

export const useAuthStore = create((set) => ({
  ...buildAuthState(initialTokens.accessToken, initialTokens.refreshToken),

  setSession: (accessToken, refreshToken) => {
    setStoredTokens(accessToken, refreshToken);
    set(buildAuthState(accessToken, refreshToken));
  },

  clearSession: () => {
    clearStoredTokens();
    set(buildAuthState(null, null));
  }
}));

