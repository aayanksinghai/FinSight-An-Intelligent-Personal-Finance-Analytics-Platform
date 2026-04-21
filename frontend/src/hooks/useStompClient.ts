import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuthStore } from '../store/authStore';
import { getStoredTokens } from '../store/tokenStore';
import type { Notification } from '../api/notificationApi';

export function useStompClient(onNotification?: (notification: Notification) => void) {
  const [isConnected, setIsConnected] = useState(false);
  const { isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (!isAuthenticated) return;

    const tokens = getStoredTokens();
    if (!tokens.accessToken) return;

    const client = new Client({
      // We use SockJS over the API Gateway proxy (mapped 8095 -> 8090)
      webSocketFactory: () => new SockJS('http://localhost:8095/ws'),
      connectHeaders: {
        Authorization: `Bearer ${tokens.accessToken}`,
      },
      debug: (str) => {
        // console.log('[STOMP]:', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    const handleMessage = (message: any) => {
      if (message.body) {
        const payload = JSON.parse(message.body) as Notification;
        
        
        
        
        if (onNotification) {
          onNotification(payload);
        }
      }
    };

    client.onConnect = () => {
      setIsConnected(true);
      
      // Subscribe to personal notification queue
      client.subscribe('/user/queue/notifications', handleMessage);
      
      // Subscribe to public announcements topic
      client.subscribe('/topic/announcements', handleMessage);
    };

    client.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    client.onWebSocketClose = () => {
      setIsConnected(false);
    };

    client.activate();

    return () => {
      void client.deactivate();
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthenticated]);

  return { isConnected };
}
