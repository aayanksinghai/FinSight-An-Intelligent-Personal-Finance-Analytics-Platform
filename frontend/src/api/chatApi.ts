import { apiClient } from './client';

export interface ChatMessage {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  intent?: string;
  rating?: 'HELPFUL' | 'NOT_HELPFUL' | null;
  createdAt: string;
}

export interface ChatSession {
  sessionId: string;
  createdAt: string;
}

export interface SendMessageResponse {
  sessionId: string;
  messageId: string;
  content: string;
  intent: string;
}

export async function sendMessage(sessionId: string | null, content: string): Promise<SendMessageResponse> {
  const res = await apiClient.post<SendMessageResponse>('/api/chat/message', { sessionId, content });
  return res.data;
}

export async function getChatHistory(sessionId: string): Promise<ChatMessage[]> {
  const res = await apiClient.get<ChatMessage[]>(`/api/chat/history/${sessionId}`);
  return res.data;
}

export async function getChatSessions(): Promise<ChatSession[]> {
  const res = await apiClient.get<ChatSession[]>('/api/chat/sessions');
  return res.data;
}

export async function rateMessage(messageId: string, rating: 'HELPFUL' | 'NOT_HELPFUL'): Promise<void> {
  await apiClient.post('/api/chat/rate', { messageId, rating });
}
