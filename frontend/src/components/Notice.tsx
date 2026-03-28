import type { ReactNode } from 'react';

interface NoticeProps {
  type?: 'error' | 'success' | 'info';
  text?: string | null;
  children?: ReactNode;
}

export default function Notice({ type = 'info', text, children }: NoticeProps) {
  const content = text ?? children;
  if (!content) return null;

  const classMap: Record<string, string> = {
    error: 'notice-error',
    success: 'notice-success',
    info: 'notice-info',
  };

  return (
    <div className={`animate-fade-in ${classMap[type] ?? 'notice-info'}`} role="alert">
      {content}
    </div>
  );
}
