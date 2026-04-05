import axios, { type InternalAxiosRequestConfig } from 'axios';
import { clearStoredTokens, getStoredTokens, setStoredTokens } from '../store/tokenStore';
import type { AuthTokenResponse } from '../types/api';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8095';

export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  headers: { 'Content-Type': 'application/json' },
});

let refreshInProgress: Promise<string> | null = null;

const PUBLIC_AUTH_PATHS = [
  '/api/users/auth/login',
  '/api/users/auth/register',
  '/api/users/auth/password-policy',
  '/api/users/auth/password-reset/request',
  '/api/users/auth/password-reset/confirm',
  '/api/users/ping',
];

function isPublicAuthPath(url = ''): boolean {
  return PUBLIC_AUTH_PATHS.some((path) => url.includes(path));
}

async function refreshAccessToken(): Promise<string> {
  const { refreshToken } = getStoredTokens();
  if (!refreshToken) throw new Error('No refresh token available');

  const response = await axios.post<AuthTokenResponse>(`${apiBaseUrl}/api/users/auth/refresh`, {
    refreshToken,
  });

  setStoredTokens(response.data.accessToken, response.data.refreshToken);
  return response.data.accessToken;
}

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (isPublicAuthPath(config.url ?? '')) {
    delete config.headers['Authorization'];
    return config;
  }

  const { accessToken } = getStoredTokens();
  if (accessToken) {
    config.headers['Authorization'] = `Bearer ${accessToken}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-explicit-any
    const original: any = error.config;
    if (
      !original ||
      // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
      original._retry ||
      // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
      error.response?.status !== 401 ||
      // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
      isPublicAuthPath(original.url) ||
      // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
      original.url?.includes('/api/users/auth/refresh')
    ) {
      return Promise.reject(error);
    }

    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
    original._retry = true;
    try {
      if (!refreshInProgress) {
        refreshInProgress = refreshAccessToken().finally(() => {
          refreshInProgress = null;
        });
      }

      const newAccessToken = await refreshInProgress;
      // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
      original.headers['Authorization'] = `Bearer ${newAccessToken}`;
      return apiClient(original);
    } catch (refreshError) {
      clearStoredTokens();
      return Promise.reject(refreshError);
    }
  },
);
