import { apiClient } from './client';

export interface Notification {
  id: string;
  ownerEmail: string;
  type: string;
  title?: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export type NotificationType = 
  | 'BUDGET_WARNING' 
  | 'BUDGET_EXCEEDED' 
  | 'ANOMALY_DETECTED' 
  | 'STRESS_SCORE_CHANGE' 
  | 'FORECAST_UPDATE' 
  | 'ANNOUNCEMENT';

export async function getNotifications(): Promise<Notification[]> {
  const { data } = await apiClient.get<Notification[]>('/api/notifications');
  return data;
}

export async function getUnreadCount(): Promise<number> {
  const { data } = await apiClient.get<{ count: number }>('/api/notifications/unread-count');
  return data.count;
}

export async function markAsRead(id: string): Promise<void> {
  await apiClient.post(`/api/notifications/${id}/read`);
}

export async function markAllAsRead(): Promise<void> {
  await apiClient.post(`/api/notifications/read-all`);
}

export async function deleteNotification(id: string): Promise<void> {
  await apiClient.delete(`/api/notifications/${id}`);
}

export async function getNotificationPreferences(): Promise<Record<string, boolean>> {
  const { data } = await apiClient.get<Record<string, boolean>>('/api/notifications/preferences');
  return data;
}

export async function updateNotificationPreferences(preferences: Record<string, boolean>): Promise<void> {
  await apiClient.put('/api/notifications/preferences', preferences);
}

export async function broadcastAnnouncement(title: string, message: string): Promise<void> {
  await apiClient.post('/api/notifications/admin/broadcast', { title, message });
}
