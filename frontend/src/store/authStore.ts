import { create } from "zustand";
import {
  clearStoredTokens,
  getStoredTokens,
  setStoredTokens,
} from "./tokenStore";
import { decodeJwtPayload } from "../utils/jwt";

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  userEmail: string | null;
  role: string | null;
  isAuthenticated: boolean;
  setSession: (accessToken: string, refreshToken: string) => void;
  logout: () => void;
}

function buildAuthState(
  accessToken: string | null,
  refreshToken: string | null,
): Omit<AuthState, "setSession" | "logout"> {
  const payload = decodeJwtPayload(accessToken);
  return {
    accessToken,
    refreshToken,
    userEmail: payload?.sub ?? null,
    role: payload?.role ?? null,
    isAuthenticated: Boolean(accessToken && refreshToken),
  };
}

const initialTokens = getStoredTokens();

export const useAuthStore = create<AuthState>((set) => ({
  ...buildAuthState(initialTokens.accessToken, initialTokens.refreshToken),

  setSession: (accessToken: string, refreshToken: string) => {
    setStoredTokens(accessToken, refreshToken);
    set(buildAuthState(accessToken, refreshToken));
  },

  logout: () => {
    clearStoredTokens();
    set(buildAuthState(null, null));
  },
}));
