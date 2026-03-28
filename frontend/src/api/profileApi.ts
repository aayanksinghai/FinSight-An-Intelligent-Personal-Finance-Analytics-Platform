import { apiClient } from './client';
import type {
  UpdateUserProfileRequest,
  UserProfileResponse,
  UserSecurityResponse,
} from '../types/api';

export async function getMyProfile(): Promise<UserProfileResponse> {
  const { data } = await apiClient.get<UserProfileResponse>('/api/users/me');
  return data;
}

export async function getSecurityOverview(): Promise<UserSecurityResponse> {
  const { data } = await apiClient.get<UserSecurityResponse>('/api/users/me/security');
  return data;
}

export async function updateProfile(
  payload: UpdateUserProfileRequest,
): Promise<UserProfileResponse> {
  const { data } = await apiClient.put<UserProfileResponse>('/api/users/me', payload);
  return data;
}

export async function deleteMyAccount(): Promise<void> {
  await apiClient.delete('/api/users/me');
}
