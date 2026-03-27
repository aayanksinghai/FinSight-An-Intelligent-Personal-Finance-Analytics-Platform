import { apiClient } from './client';

export async function getMyProfile() {
  const { data } = await apiClient.get('/api/users/me');
  return data;
}

export async function getSecurityOverview() {
  const { data } = await apiClient.get('/api/users/me/security');
  return data;
}

export async function updateProfile(payload) {
  const { data } = await apiClient.put('/api/users/me', payload);
  return data;
}

export async function deleteMyAccount() {
  await apiClient.delete('/api/users/me');
}

