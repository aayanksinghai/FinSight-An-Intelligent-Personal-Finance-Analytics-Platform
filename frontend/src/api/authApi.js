import { apiClient } from './client';

export async function login(email, password) {
  const { data } = await apiClient.post('/api/users/auth/login', { email, password });
  return data;
}

export async function register(email, password) {
  const { data } = await apiClient.post('/api/users/auth/register', { email, password });
  return data;
}

export async function logout() {
  await apiClient.post('/api/users/auth/logout');
}

export async function logoutAll() {
  await apiClient.post('/api/users/auth/logout-all');
}

export async function changePassword(currentPassword, newPassword) {
  await apiClient.post('/api/users/auth/change-password', { currentPassword, newPassword });
}

export async function requestPasswordReset(email) {
  const { data } = await apiClient.post('/api/users/auth/password-reset/request', { email });
  return data;
}

export async function confirmPasswordReset(resetToken, newPassword) {
  await apiClient.post('/api/users/auth/password-reset/confirm', { resetToken, newPassword });
}

