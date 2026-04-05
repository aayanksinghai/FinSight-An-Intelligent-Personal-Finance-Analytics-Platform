import { apiClient } from './client';

export interface Notification {
  id: string;
  ownerEmail: string;
  type: string;
  message: string;
  read: boolean; // mapped from isRead by axios interceptors/basic json
  createdAt: string;
}

export async function getNotifications(): Promise<Notification[]> {
  const { data } = await apiClient.get<Notification[]>('/api/notifications');
  return data;
}

export async function markAsRead(id: string): Promise<void> {
  await apiClient.post(`/api/notifications/${id}/read`);
}

export async function markAllAsRead(): Promise<void> {
  await apiClient.post(`/api/notifications/read-all`);
}
