const ACCESS_KEY = 'finsight_access_token';
const REFRESH_KEY = 'finsight_refresh_token';

export interface StoredTokens {
  accessToken: string | null;
  refreshToken: string | null;
}

export function getStoredTokens(): StoredTokens {
  return {
    accessToken: localStorage.getItem(ACCESS_KEY),
    refreshToken: localStorage.getItem(REFRESH_KEY),
  };
}

export function setStoredTokens(accessToken: string, refreshToken: string): void {
  if (accessToken) localStorage.setItem(ACCESS_KEY, accessToken);
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearStoredTokens(): void {
  localStorage.removeItem(ACCESS_KEY);
  localStorage.removeItem(REFRESH_KEY);
}
