import axios from 'axios';
import { clearStoredTokens, getStoredTokens, setStoredTokens } from '../store/tokenStore';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090';

export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json'
  }
});

let refreshInProgress = null;

const publicAuthPaths = [
  '/api/users/auth/login',
  '/api/users/auth/register',
  '/api/users/auth/password-policy',
  '/api/users/auth/password-reset/request',
  '/api/users/auth/password-reset/confirm',
  '/api/users/ping'
];

function isPublicAuthPath(url = '') {
  return publicAuthPaths.some((path) => url.includes(path));
}

async function refreshAccessToken() {
  const { refreshToken } = getStoredTokens();
  if (!refreshToken) {
    throw new Error('No refresh token available');
  }

  const response = await axios.post(`${apiBaseUrl}/api/users/auth/refresh`, {
    refreshToken
  });

  setStoredTokens(response.data.accessToken, response.data.refreshToken);
  return response.data.accessToken;
}

apiClient.interceptors.request.use((config) => {
  if (isPublicAuthPath(config.url)) {
    delete config.headers.Authorization;
    return config;
  }

  const { accessToken } = getStoredTokens();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    if (
      !original ||
      original._retry ||
      error.response?.status !== 401 ||
      isPublicAuthPath(original.url) ||
      original.url?.includes('/api/users/auth/refresh')
    ) {
      return Promise.reject(error);
    }

    original._retry = true;
    try {
      if (!refreshInProgress) {
        refreshInProgress = refreshAccessToken().finally(() => {
          refreshInProgress = null;
        });
      }

      const newAccessToken = await refreshInProgress;
      original.headers.Authorization = `Bearer ${newAccessToken}`;
      return apiClient(original);
    } catch (refreshError) {
      clearStoredTokens();
      return Promise.reject(refreshError);
    }
  }
);

