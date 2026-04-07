import { useState, useEffect, useRef, useCallback } from 'react';
import { sendMessage, getChatHistory, rateMessage } from '../api/chatApi';
import type { ChatMessage } from '../api/chatApi';

const SUGGESTED_PROMPTS = [
  { icon: '📊', label: 'Spending Summary', prompt: 'How much did I spend in the last 30 days?' },
  { icon: '📂', label: 'Category Breakdown', prompt: 'What are my top spending categories this month?' },
  { icon: '📋', label: 'Budget Status', prompt: 'Am I over budget in any category this month?' },
  { icon: '🔮', label: 'Forecast', prompt: 'What will my spending be next month?' },
  { icon: '🚨', label: 'Anomalies', prompt: 'Why was my account flagged for anomalies?' },
  { icon: '🤔', label: 'What-If', prompt: 'If I reduce my spending by 20%, how much do I save?' },
];

function TypingIndicator() {
  return (
    <div className="flex items-end gap-3 mb-4">
      <div className="flex-shrink-0 h-8 w-8 rounded-full bg-gradient-to-br from-brand to-purple-500 flex items-center justify-center text-xs font-bold text-white shadow-glow">
        AI
      </div>
      <div className="glass-card px-4 py-3 rounded-2xl rounded-bl-sm max-w-xs">
        <div className="flex gap-1 items-center h-4">
          <span className="h-2 w-2 rounded-full bg-brand animate-bounce" style={{ animationDelay: '0ms' }} />
          <span className="h-2 w-2 rounded-full bg-brand animate-bounce" style={{ animationDelay: '150ms' }} />
          <span className="h-2 w-2 rounded-full bg-brand animate-bounce" style={{ animationDelay: '300ms' }} />
        </div>
      </div>
    </div>
  );
}

function MessageBubble({ msg, onRate }: { msg: ChatMessage; onRate: (id: string, r: 'HELPFUL' | 'NOT_HELPFUL') => void }) {
  const isUser = msg.role === 'USER';

  const formatContent = (text: string) => {
    return text.split('\n').map((line, i) => {
      const boldLine = line.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
      return <p key={i} className="leading-relaxed" dangerouslySetInnerHTML={{ __html: boldLine || '&nbsp;' }} />;
    });
  };

  return (
    <div className={`flex items-end gap-3 mb-4 ${isUser ? 'flex-row-reverse' : ''}`}>
      {/* Avatar */}
      <div className={`flex-shrink-0 h-8 w-8 rounded-full flex items-center justify-center text-xs font-bold shadow-lg ${
        isUser
          ? 'bg-gradient-to-br from-brand to-brand-purple text-white'
          : 'bg-gradient-to-br from-purple-600 to-indigo-600 text-white shadow-glow'
      }`}>
        {isUser ? 'ME' : 'AI'}
      </div>

      {/* Bubble */}
      <div className={`max-w-[70%] group ${isUser ? 'items-end' : 'items-start'} flex flex-col gap-1`}>
        <div className={`px-4 py-3 rounded-2xl text-sm ${
          isUser
            ? 'bg-gradient-to-br from-brand to-brand-purple text-white rounded-br-sm shadow-glow'
            : 'glass-card rounded-bl-sm border border-white/10 text-[#edf2ff]'
        }`}>
          {formatContent(msg.content)}
        </div>

        {/* Rating bar — only for assistant messages */}
        {!isUser && (
          <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-all duration-200 pl-1">
            <span className="text-[10px] text-muted">Was this helpful?</span>
            <button
              onClick={() => onRate(msg.id, 'HELPFUL')}
              className={`text-sm px-2 py-0.5 rounded-lg transition-all duration-150 ${
                msg.rating === 'HELPFUL'
                  ? 'bg-green-500/20 text-green-400 ring-1 ring-green-400/40'
                  : 'hover:bg-green-500/10 text-muted hover:text-green-400'
              }`}
              title="Helpful"
            >
              👍
            </button>
            <button
              onClick={() => onRate(msg.id, 'NOT_HELPFUL')}
              className={`text-sm px-2 py-0.5 rounded-lg transition-all duration-150 ${
                msg.rating === 'NOT_HELPFUL'
                  ? 'bg-red-500/20 text-red-400 ring-1 ring-red-400/40'
                  : 'hover:bg-red-500/10 text-muted hover:text-red-400'
              }`}
              title="Not helpful"
            >
              👎
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sessionId, setSessionId] = useState<string | null>(() => sessionStorage.getItem('chat_session_id'));
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  // Restore chat history on mount
  useEffect(() => {
    if (sessionId) {
      getChatHistory(sessionId)
        .then(setMessages)
        .catch(() => {
          sessionStorage.removeItem('chat_session_id');
          setSessionId(null);
        });
    }
  }, [sessionId]);

  // Auto scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  const handleSend = useCallback(async (content: string) => {
    if (!content.trim() || isLoading) return;

    const userMsg: ChatMessage = {
      id: `temp-${Date.now()}`,
      role: 'USER',
      content: content.trim(),
      createdAt: new Date().toISOString(),
    };
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setIsLoading(true);

    try {
      const res = await sendMessage(sessionId, content.trim());

      // Persist session ID
      if (!sessionId || sessionId !== res.sessionId) {
        setSessionId(res.sessionId);
        sessionStorage.setItem('chat_session_id', res.sessionId);
      }

      const assistantMsg: ChatMessage = {
        id: res.messageId,
        role: 'ASSISTANT',
        content: res.content,
        intent: res.intent,
        createdAt: new Date().toISOString(),
      };
      setMessages(prev => [...prev, assistantMsg]);
    } catch {
      const errMsg: ChatMessage = {
        id: `err-${Date.now()}`,
        role: 'ASSISTANT',
        content: '⚠️ Sorry, I couldn\'t process your request. Please check your connection and try again.',
        createdAt: new Date().toISOString(),
      };
      setMessages(prev => [...prev, errMsg]);
    } finally {
      setIsLoading(false);
      inputRef.current?.focus();
    }
  }, [sessionId, isLoading]);

  const handleRate = useCallback(async (messageId: string, rating: 'HELPFUL' | 'NOT_HELPFUL') => {
    setMessages(prev => prev.map(m => m.id === messageId ? { ...m, rating } : m));
    try {
      await rateMessage(messageId, rating);
    } catch {
      // Revert on failure
      setMessages(prev => prev.map(m => m.id === messageId ? { ...m, rating: undefined } : m));
    }
  }, []);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      void handleSend(input);
    }
  };

  const startNewChat = () => {
    setMessages([]);
    setSessionId(null);
    sessionStorage.removeItem('chat_session_id');
    inputRef.current?.focus();
  };

  const isEmpty = messages.length === 0 && !isLoading;

  return (
    <div className="flex flex-col h-[calc(100vh-8rem)] gap-0">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-xl font-bold text-[#edf2ff] flex items-center gap-2">
            <span className="h-8 w-8 rounded-xl bg-gradient-to-br from-brand to-purple-500 flex items-center justify-center text-sm shadow-glow">🤖</span>
            AI Financial Assistant
          </h1>
          <p className="text-xs text-muted mt-1">Ask anything about your finances — powered by your real data</p>
        </div>
        <button
          onClick={startNewChat}
          className="text-xs text-muted hover:text-[#edf2ff] border border-stroke hover:border-brand/50 px-3 py-1.5 rounded-lg transition-all duration-200"
        >
          + New Chat
        </button>
      </div>

      {/* Message Area */}
      <div className="glass-card flex-1 flex flex-col overflow-hidden">
        <div className="flex-1 overflow-y-auto p-5 scroll-smooth">
          {/* Empty State */}
          {isEmpty && (
            <div className="flex flex-col items-center justify-center h-full gap-6 text-center">
              <div className="h-16 w-16 rounded-2xl bg-gradient-to-br from-brand to-purple-500 flex items-center justify-center text-2xl shadow-glow animate-pulse-slow">
                🤖
              </div>
              <div>
                <h2 className="text-[#edf2ff] font-semibold text-lg">Your Financial AI Assistant</h2>
                <p className="text-muted text-sm mt-1 max-w-sm">
                  Ask me about your spending, budgets, anomalies, or planning scenarios. I use your real transaction data to give you accurate insights.
                </p>
              </div>

              {/* Quick-start chips */}
              <div className="grid grid-cols-2 gap-2 w-full max-w-md mt-2">
                {SUGGESTED_PROMPTS.map(p => (
                  <button
                    key={p.label}
                    onClick={() => void handleSend(p.prompt)}
                    className="glass-card border border-white/10 hover:border-brand/50 px-3 py-2.5 rounded-xl text-left transition-all duration-200 hover:shadow-glow group"
                  >
                    <span className="text-xl">{p.icon}</span>
                    <p className="text-xs font-medium text-[#edf2ff] mt-1 group-hover:text-brand transition-colors">{p.label}</p>
                    <p className="text-[10px] text-muted truncate">{p.prompt}</p>
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Messages */}
          {messages.map(msg => (
            <MessageBubble key={msg.id} msg={msg} onRate={handleRate} />
          ))}

          {/* Typing Indicator */}
          {isLoading && <TypingIndicator />}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Area */}
        <div className="border-t border-white/5 p-4">
          <div className="flex items-end gap-3">
            <textarea
              ref={inputRef}
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask about your spending, budgets, or run a what-if scenario..."
              rows={1}
              className="flex-1 bg-white/[0.04] border border-stroke rounded-xl px-4 py-3 text-sm text-[#edf2ff] placeholder-muted resize-none focus:outline-none focus:border-brand/50 focus:ring-1 focus:ring-brand/30 transition-all duration-200"
              style={{ minHeight: '48px', maxHeight: '120px' }}
              onInput={e => {
                const target = e.target as HTMLTextAreaElement;
                target.style.height = 'auto';
                target.style.height = `${Math.min(target.scrollHeight, 120)}px`;
              }}
              disabled={isLoading}
            />
            <button
              onClick={() => void handleSend(input)}
              disabled={!input.trim() || isLoading}
              className="h-12 w-12 flex-shrink-0 rounded-xl bg-gradient-to-br from-brand to-brand-purple text-white flex items-center justify-center disabled:opacity-40 disabled:cursor-not-allowed hover:shadow-glow transition-all duration-200 active:scale-95"
              title="Send (Enter)"
            >
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
              </svg>
            </button>
          </div>
          <p className="text-[10px] text-muted mt-2 text-center">
            Press <kbd className="bg-white/10 px-1 rounded text-[10px]">Enter</kbd> to send · <kbd className="bg-white/10 px-1 rounded text-[10px]">Shift+Enter</kbd> for new line
          </p>
        </div>
      </div>
    </div>
  );
}
