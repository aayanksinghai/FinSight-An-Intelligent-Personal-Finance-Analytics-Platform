import { apiClient } from './client';
import type { AdminUserResponse, Page } from '../types/api';

export async function listUsers(
  status: 'all' | 'active' | 'inactive' = 'all',
  page = 0,
  size = 20,
): Promise<Page<AdminUserResponse>> {
  const { data } = await apiClient.get<Page<AdminUserResponse>>('/api/admin/users', {
    params: { status, page, size },
  });
  return data;
}

export async function deactivateUser(email: string): Promise<void> {
  await apiClient.patch(`/api/admin/users/${encodeURIComponent(email)}/deactivate`);
}

export async function activateUser(email: string): Promise<void> {
  await apiClient.patch(`/api/admin/users/${encodeURIComponent(email)}/activate`);
}
