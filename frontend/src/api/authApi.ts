import { apiClient } from './client';
import type {
  AuthTokenResponse,
  PasswordPolicyResponse,
  PasswordResetResponse,
  RegisterResponse,
} from '../types/api';

export async function login(email: string, password: string): Promise<AuthTokenResponse> {
  const { data } = await apiClient.post<AuthTokenResponse>('/api/users/auth/login', {
    email,
    password,
  });
  return data;
}

export async function register(email: string, password: string): Promise<RegisterResponse> {
  const { data } = await apiClient.post<RegisterResponse>('/api/users/auth/register', {
    email,
    password,
  });
  return data;
}

export async function logout(): Promise<void> {
  await apiClient.post('/api/users/auth/logout');
}

export async function logoutAll(): Promise<void> {
  await apiClient.post('/api/users/auth/logout-all');
}

export async function changePassword(
  currentPassword: string,
  newPassword: string,
): Promise<void> {
  await apiClient.post('/api/users/auth/change-password', { currentPassword, newPassword });
}

export async function requestPasswordReset(email: string): Promise<PasswordResetResponse> {
  const { data } = await apiClient.post<PasswordResetResponse>(
    '/api/users/auth/password-reset/request',
    { email },
  );
  return data;
}

export async function confirmPasswordReset(
  resetToken: string,
  newPassword: string,
): Promise<void> {
  await apiClient.post('/api/users/auth/password-reset/confirm', { resetToken, newPassword });
}

export async function getPasswordPolicy(): Promise<PasswordPolicyResponse> {
  const { data } = await apiClient.get<PasswordPolicyResponse>('/api/users/auth/password-policy');
  return data;
}
