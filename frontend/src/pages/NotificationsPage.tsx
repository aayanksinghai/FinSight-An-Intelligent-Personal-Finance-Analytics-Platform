import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  getNotifications, 
  markAsRead, 
  markAllAsRead, 
  deleteNotification,
  type Notification 
} from '../api/notificationApi';
import NotificationPreferences from '../components/NotificationPreferences';

export default function NotificationsPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<'ALL' | 'UNREAD'>('ALL');
  const [showPrefs, setShowPrefs] = useState(false);

  const { data: notifications = [], isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: getNotifications,
  });

  const markReadMutation = useMutation({
    mutationFn: markAsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });

  const markAllMutation = useMutation({
    mutationFn: markAllAsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteNotification,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });

  const filtered = filter === 'UNREAD' 
    ? notifications.filter(n => !n.read) 
    : notifications;

  function getIcon(type: string) {
    switch (type) {
      case 'BUDGET_WARNING': return '⚠️';
      case 'BUDGET_EXCEEDED': return '🛑';
      case 'ANOMALY_DETECTED': return '🚨';
      case 'STRESS_SCORE_CHANGE': return '📊';
      case 'FORECAST_UPDATE': return '📈';
      case 'ANNOUNCEMENT': return '📢';
      default: return '🔔';
    }
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-fade-in">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-[#edf2ff]">Notification Inbox</h1>
          <p className="text-muted text-sm">Stay updated with your financial alerts and platform news.</p>
        </div>
        <div className="flex gap-2">
          <button 
            onClick={() => setShowPrefs(!showPrefs)}
            className="btn-secondary text-xs py-2"
          >
            {showPrefs ? 'View Inbox' : 'Preferences'}
          </button>
          {notifications.some(n => !n.read) && (
            <button 
              onClick={() => markAllMutation.mutate()}
              className="px-4 py-2 bg-brand/10 text-brand text-xs font-semibold rounded-lg hover:bg-brand/20 transition-colors"
            >
              Mark all as read
            </button>
          )}
        </div>
      </div>

      {showPrefs ? (
        <NotificationPreferences />
      ) : (
        <div className="glass-card overflow-hidden">
          <div className="flex border-b border-white/5">
            <button 
              onClick={() => setFilter('ALL')}
              className={`px-6 py-3 text-sm font-medium transition-colors ${filter === 'ALL' ? 'text-brand border-b-2 border-brand' : 'text-muted hover:text-white'}`}
            >
              All
            </button>
            <button 
              onClick={() => setFilter('UNREAD')}
              className={`px-6 py-3 text-sm font-medium transition-colors ${filter === 'UNREAD' ? 'text-brand border-b-2 border-brand' : 'text-muted hover:text-white'}`}
            >
              Unread
            </button>
          </div>

          {isLoading ? (
            <div className="p-12 text-center text-muted">Loading notifications...</div>
          ) : filtered.length === 0 ? (
            <div className="p-20 text-center space-y-3">
              <div className="text-4xl">📭</div>
              <p className="text-muted">No notifications found.</p>
            </div>
          ) : (
            <div className="divide-y divide-white/5">
              {filtered.map(n => (
                <div 
                  key={n.id} 
                  className={`p-5 flex gap-4 transition-colors hover:bg-white/[0.02] ${!n.read ? 'bg-brand/5' : ''}`}
                >
                  <div className="text-2xl flex-shrink-0 bg-white/5 h-12 w-12 rounded-xl flex items-center justify-center">
                    {getIcon(n.type)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-start">
                      <h3 className={`text-sm font-semibold truncate ${!n.read ? 'text-[#edf2ff]' : 'text-muted'}`}>
                        {n.title || n.type.replace(/_/g, ' ')}
                      </h3>
                      <span className="text-[10px] text-muted whitespace-nowrap ml-4">
                        {new Date(n.createdAt).toLocaleString()}
                      </span>
                    </div>
                    <p className={`text-sm mt-1 leading-relaxed ${!n.read ? 'text-[#c1ccf5]' : 'text-muted/70'}`}>
                      {n.message}
                    </p>
                    <div className="flex gap-4 mt-3">
                      {!n.read && (
                        <button 
                          onClick={() => markReadMutation.mutate(n.id)}
                          className="text-[11px] text-brand hover:underline font-medium"
                        >
                          Mark as read
                        </button>
                      )}
                      <button 
                        onClick={() => deleteMutation.mutate(n.id)}
                        className="text-[11px] text-red-400/70 hover:text-red-400 hover:underline font-medium"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                  {!n.read && (
                    <div className="flex-shrink-0 self-center">
                      <div className="h-2 w-2 rounded-full bg-brand shadow-glow" />
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
