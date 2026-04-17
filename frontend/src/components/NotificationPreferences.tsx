import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getNotificationPreferences, updateNotificationPreferences } from '../api/notificationApi';
import toast from 'react-hot-toast';

const NOTIFICATION_TYPES = [
  { id: 'BUDGET_WARNING', label: 'Budget Warnings', desc: 'Alerts when you approach your set budget limits.' },
  { id: 'BUDGET_EXCEEDED', label: 'Budget Exceeded', desc: 'Urgent alerts when a category spend exceeds its limit.' },
  { id: 'ANOMALY_DETECTED', label: 'Unusual Transactions', desc: 'Alerts for transactions that don’t fit your usual patterns.' },
  { id: 'STRESS_SCORE_CHANGE', label: 'Stress Score Changes', desc: 'Monthly updates when your financial stress score shifts significantly.' },
  { id: 'FORECAST_UPDATE', label: 'Forecast Availability', desc: 'Notifications when new spending projections are ready.' },
  { id: 'ANNOUNCEMENT', label: 'Platform Announcements', desc: 'Updates about new features and platform news.' },
];

export default function NotificationPreferences() {
  const queryClient = useQueryClient();

  const { data: prefs = {}, isLoading } = useQuery({
    queryKey: ['notification-preferences'],
    queryFn: getNotificationPreferences,
  });

  const updateMutation = useMutation({
    mutationFn: updateNotificationPreferences,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-preferences'] });
      toast.success('Preferences updated');
    },
    onError: () => toast.error('Failed to update preferences'),
  });

  const handleToggle = (type: string, enabled: boolean) => {
    updateMutation.mutate({ ...prefs, [type]: enabled });
  };

  if (isLoading) return <div className="p-8 text-center text-muted">Loading preferences...</div>;

  return (
    <div className="glass-card animate-slide-up">
      <div className="p-6 border-b border-white/5">
        <h2 className="text-lg font-semibold text-[#edf2ff]">Notification Preferences</h2>
        <p className="text-muted text-xs mt-1">Configure which alerts you want to receive in real-time.</p>
      </div>

      <div className="divide-y divide-white/5">
        {NOTIFICATION_TYPES.map(type => (
          <div key={type.id} className="p-6 flex items-center justify-between hover:bg-white/[0.01] transition-colors">
            <div className="space-y-1 pr-4">
              <h3 className="text-sm font-medium text-[#edf2ff]">{type.label}</h3>
              <p className="text-xs text-muted leading-relaxed max-w-md">{type.desc}</p>
            </div>
            
            <button
              onClick={() => handleToggle(type.id, !prefs[type.id])}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none ${
                prefs[type.id] !== false ? 'bg-brand' : 'bg-white/10'
              }`}
            >
              <span
                className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                  prefs[type.id] !== false ? 'translate-x-6' : 'translate-x-1'
                }`}
              />
            </button>
          </div>
        ))}
      </div>

      <div className="p-6 bg-white/[0.02] border-t border-white/5">
        <p className="text-[10px] text-muted italic">
          * Some critical security notifications may ignore these settings for your protection.
        </p>
      </div>
    </div>
  );
}
