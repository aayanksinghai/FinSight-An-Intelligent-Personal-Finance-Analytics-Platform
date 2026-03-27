import { apiClient } from './client';

export async function listUsers(status = 'all', page = 0, size = 20) {
  const { data } = await apiClient.get('/api/admin/users', {
    params: { status, page, size }
  });
  return data;
}

export async function deactivateUser(email) {
  await apiClient.patch(`/api/admin/users/${encodeURIComponent(email)}/deactivate`);
}

export async function activateUser(email) {
  await apiClient.patch(`/api/admin/users/${encodeURIComponent(email)}/activate`);
}

