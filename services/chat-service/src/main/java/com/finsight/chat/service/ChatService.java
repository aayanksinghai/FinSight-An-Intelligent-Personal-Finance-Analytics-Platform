package com.finsight.chat.service;

import com.finsight.chat.client.BudgetClient;
import com.finsight.chat.client.GeminiClient;
import com.finsight.chat.client.TransactionClient;
import com.finsight.chat.domain.ChatMessage;
import com.finsight.chat.domain.ChatRating;
import com.finsight.chat.domain.ChatSession;
import com.finsight.chat.intent.IntentClassifier;
import com.finsight.chat.model.*;
import com.finsight.chat.persistence.ChatMessageRepository;
import com.finsight.chat.persistence.ChatRatingRepository;
import com.finsight.chat.persistence.ChatSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatService.class);

    // ── System prompt given to Gemini (RAG system prompt) ────────────────────
    private static final String SYSTEM_PROMPT = """
        You are a smart, friendly, and conversational AI Financial Assistant called "FinSight AI".
        
        Your goal is to help users understand their finances in a natural, human-like way — not like a dashboard or report.
        
        Guidelines:
        - Always respond conversationally, like a helpful friend who happens to be great with money.
        - Interpret the financial data and explain what it means for the user, don't just report numbers.
        - Add helpful suggestions or observations when relevant.
        - Keep responses concise (2-4 sentences usually), but meaningful.
        - Use a warm, encouraging, and supportive tone.
        - When something is good, celebrate it. When something needs attention, explain clearly + suggest a fix.
        - Use emojis sparingly to add warmth (1-2 per response max).
        - Format numbers with ₹ and commas for readability.
        - Answer the user's actual question directly before adding extra insight.
        
        Style Rules:
        - ❌ Do NOT say: "Food & Dining: ₹0 / ₹50 (0%) OK"
        - ✅ DO say: "You're well within your Food & Dining budget — you haven't spent anything there yet this month!"
        - ❌ Do NOT list data robotically.
        - ✅ DO interpret what the numbers mean for the user.
        - ❌ Do NOT give the same answer regardless of how a question is phrased.
        - ✅ DO tailor your response to the specific wording and focus of the query.
        
        If you don't have enough data to answer accurately, say so honestly and suggest what the user should do (e.g., upload a bank statement).
        
        Keep your response under 200 words unless the question genuinely requires more detail.
        """;

    private final IntentClassifier intentClassifier;
    private final TransactionClient transactionClient;
    private final BudgetClient budgetClient;
    private final GeminiClient geminiClient;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatRatingRepository ratingRepo;

    public ChatService(IntentClassifier intentClassifier,
                       TransactionClient transactionClient,
                       BudgetClient budgetClient,
                       GeminiClient geminiClient,
                       ChatSessionRepository sessionRepo,
                       ChatMessageRepository messageRepo,
                       ChatRatingRepository ratingRepo) {
        this.intentClassifier = intentClassifier;
        this.transactionClient = transactionClient;
        this.budgetClient = budgetClient;
        this.geminiClient = geminiClient;
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.ratingRepo = ratingRepo;
    }

    @Transactional
    public ChatResponse handleMessage(String ownerEmail, String bearerToken, ChatRequest request) {
        // 1. Resolve or create session
        ChatSession session = resolveSession(request.sessionId(), ownerEmail);

        // 2. Persist user message
        ChatMessage userMsg = new ChatMessage(session.getId(), "USER", request.content(), null);
        messageRepo.save(userMsg);

        // 3. Classify intent
        ChatIntent intent = intentClassifier.classify(request.content());

        // 4. Fetch relevant financial data for this intent
        String dataContext = fetchDataContext(intent, request.content(), bearerToken);
        log.info("Data context for user {}: {}", ownerEmail, dataContext);

        // 5. Generate natural-language response via Gemini (RAG)
        String responseText = generateWithGemini(request.content(), dataContext, intent);

        // 6. Persist assistant message
        ChatMessage assistantMsg = new ChatMessage(session.getId(), "ASSISTANT", responseText, intent.name());
        messageRepo.save(assistantMsg);

        return new ChatResponse(session.getId().toString(), assistantMsg.getId().toString(), responseText, intent.name());
    }

    @Transactional
    public void rateMessage(String ownerEmail, RatingRequest request) {
        UUID msgId = UUID.fromString(request.messageId());
        ChatRating existing = ratingRepo.findById(msgId).orElse(null);
        if (existing != null) {
            existing.setRating(request.rating());
            ratingRepo.save(existing);
        } else {
            ratingRepo.save(new ChatRating(msgId, request.rating()));
        }
    }

    public List<ChatMessageDto> getHistory(String ownerEmail, String sessionId) {
        sessionRepo.findByIdAndOwnerEmail(UUID.fromString(sessionId), ownerEmail)
            .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        List<ChatMessage> messages = messageRepo.findBySessionIdOrderByCreatedAtAsc(UUID.fromString(sessionId));
        Map<UUID, String> ratings = ratingRepo.findAllById(
            messages.stream().map(ChatMessage::getId).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(ChatRating::getMessageId, ChatRating::getRating));

        return messages.stream()
            .map(m -> new ChatMessageDto(
                m.getId().toString(),
                m.getRole(),
                m.getContent(),
                m.getIntent(),
                ratings.get(m.getId()),
                m.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getSessions(String ownerEmail) {
        return sessionRepo.findByOwnerEmailOrderByCreatedAtDesc(ownerEmail).stream()
            .map(s -> Map.<String, Object>of(
                "sessionId", s.getId().toString(),
                "createdAt", s.getCreatedAt().toString()
            ))
            .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // RAG Pipeline: Intent → Data → Prompt → LLM
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Fetches the relevant financial data for the given intent and formats it
     * as a structured plain-text context block for the LLM.
     */
    private String fetchDataContext(ChatIntent intent, String query, String bearerToken) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("Current month: ").append(java.time.YearMonth.now()).append("\n");
        ctx.append("Detected intent: ").append(intent.name()).append("\n\n");

        try {
            switch (intent) {
                case SPENDING_SUMMARY, CATEGORY_COMPARISON, FORECAST_INQUIRY -> {
                    Instant to = Instant.now();
                    Instant from = to.minus(730, ChronoUnit.DAYS);
                    appendSummaryContext(ctx, bearerToken, from, to);
                    appendCategoryContext(ctx, bearerToken, from, to);
                }
                case BUDGET_STATUS -> {
                    appendBudgetContext(ctx, bearerToken);
                    // Also get spending for comparison
                    Instant to = Instant.now();
                    Instant from = to.minus(730, ChronoUnit.DAYS);
                    appendCategoryContext(ctx, bearerToken, from, to);
                }
                case WHAT_IF -> {
                    Instant to = Instant.now();
                    Instant from = to.minus(730, ChronoUnit.DAYS);
                    appendSummaryContext(ctx, bearerToken, from, to);
                    appendCategoryContext(ctx, bearerToken, from, to);
                    // Extract percentage from query if present
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*%").matcher(query);
                    double pct = m.find() ? Double.parseDouble(m.group(1)) : 20;
                    ctx.append("User wants a what-if scenario with ").append(pct).append("% reduction.\n");
                }
                case ANOMALY_EXPLANATION -> {
                    ctx.append("The user is asking about anomaly detection alerts.\n");
                    ctx.append("Anomaly detection is active and monitors for unusual transaction patterns.\n");
                    ctx.append("Users can view active alerts in the notification inbox (bell icon).\n");
                }
                default -> {
                    // For UNKNOWN, try to get basic summary anyway
                    Instant to = Instant.now();
                    Instant from = to.minus(730, ChronoUnit.DAYS);
                    appendSummaryContext(ctx, bearerToken, from, to);
                }
            }
        } catch (Exception e) {
            ctx.append("Note: Could not fetch live financial data (").append(e.getMessage()).append(")\n");
            ctx.append("Advise the user to check their connection or upload a bank statement.\n");
        }

        return ctx.toString();
    }

    private void appendSummaryContext(StringBuilder ctx, String bearerToken, Instant from, Instant to) {
        List<Map<String, Object>> summary = transactionClient.getSummary(bearerToken, from, to);
        if (summary == null || summary.isEmpty()) {
            ctx.append("No transaction summary available for this period.\n");
            return;
        }
        ctx.append("Transaction summary (last 2 years):\n");
        double totalDebit = 0, totalCredit = 0;
        for (Map<String, Object> row : summary) {
            String type = String.valueOf(row.getOrDefault("type", ""));
            double amount = toDouble(row.get("totalAmount"));
            long count = toLong(row.get("transactionCount"));
            if ("DEBIT".equalsIgnoreCase(type)) totalDebit = amount;
            if ("CREDIT".equalsIgnoreCase(type)) totalCredit = amount;
            ctx.append("- ").append(type).append(": ₹").append(String.format("%.2f", amount))
               .append(" across ").append(count).append(" transactions\n");
        }
        double net = totalCredit - totalDebit;
        ctx.append("- Net cash flow: ₹").append(String.format("%.2f", net)).append(net >= 0 ? " (positive)\n" : " (negative — spent more than earned)\n");
    }

    private void appendCategoryContext(StringBuilder ctx, String bearerToken, Instant from, Instant to) {
        List<Map<String, Object>> cats = transactionClient.getCategories(bearerToken, from, to);
        if (cats == null || cats.isEmpty()) {
            ctx.append("No category breakdown available.\n");
            return;
        }
        cats.sort((a, b) -> Double.compare(toDouble(b.get("totalAmount")), toDouble(a.get("totalAmount"))));
        double total = cats.stream().mapToDouble(c -> toDouble(c.get("totalAmount"))).sum();
        ctx.append("Spending by category (last 2 years, sorted by highest):\n");
        for (Map<String, Object> cat : cats) {
            String name = String.valueOf(cat.getOrDefault("category", "Other"));
            double amt = toDouble(cat.get("totalAmount"));
            double pct = total > 0 ? (amt / total * 100) : 0;
            ctx.append("- ").append(name).append(": ₹").append(String.format("%.2f", amt))
               .append(" (").append(String.format("%.1f", pct)).append("% of total spending)\n");
        }
    }

    private void appendBudgetContext(StringBuilder ctx, String bearerToken) {
        List<Map<String, Object>> budgets = budgetClient.getCurrentMonthBudgets(bearerToken);
        if (budgets == null || budgets.isEmpty()) {
            ctx.append("No budgets have been set for this month.\n");
            return;
        }
        ctx.append("Budgets for this month:\n");
        for (Map<String, Object> b : budgets) {
            String cat = String.valueOf(b.getOrDefault("categoryName", "Unknown"));
            double limit = toDouble(b.get("limitAmount"));
            double spent = toDouble(b.get("spentAmount"));
            double remaining = limit - spent;
            double pct = limit > 0 ? (spent / limit * 100) : 0;
            String status = pct >= 100 ? "OVER BUDGET" : pct >= 80 ? "WARNING (nearing limit)" : "within budget";
            ctx.append("- ").append(cat).append(": spent ₹").append(String.format("%.2f", spent))
               .append(" of ₹").append(String.format("%.2f", limit))
               .append(" limit (").append(String.format("%.0f", pct)).append("%) — ").append(status)
               .append(remaining >= 0 ? ", ₹" + String.format("%.2f", remaining) + " remaining\n"
                                     : ", ₹" + String.format("%.2f", Math.abs(remaining)) + " over limit\n");
        }
    }

    /**
     * Calls Gemini with the assembled context prompt.
     * If Gemini is unavailable (no API key), falls back to a friendly default message.
     */
    private String generateWithGemini(String userMessage, String dataContext, ChatIntent intent) {
        String geminiResponse = geminiClient.generate(SYSTEM_PROMPT, userMessage, dataContext);
        if (geminiResponse != null && !geminiResponse.isBlank()) {
            return geminiResponse.trim();
        }
        // Graceful fallback: if no API key configured, return a helpful message
        return fallbackResponse(intent, dataContext);
    }

    /**
     * Fallback template when Gemini is not configured.
     * More conversational than the old templates by reading data context as plain text.
     */
    private String fallbackResponse(ChatIntent intent, String dataContext) {
        boolean hasData = !dataContext.contains("No transaction") && !dataContext.contains("No category") && !dataContext.contains("No budget");
        return switch (intent) {
            case SPENDING_SUMMARY -> hasData
                ? "Here's a quick look at your finances for the last 30 days 📊\n\n" + dataContext + "\n💡 Tip: Set budgets in the Budgets page to track these figures more actively."
                : "I don't see any transactions yet. Try uploading a bank statement first and I'll give you a full picture!";
            case CATEGORY_COMPARISON -> hasData
                ? "Here's how your spending breaks down by category 📂\n\n" + dataContext
                : "No categorized spending yet. Upload a statement and the AI will automatically categorize your transactions!";
            case BUDGET_STATUS -> hasData
                ? "Here's your budget health for this month 📋\n\n" + dataContext
                : "You haven't set any budgets yet. Head to the Budgets page to set category limits!";
            case FORECAST_INQUIRY -> "Based on your recent patterns:\n\n" + dataContext + "\n\n💡 For more accurate forecasts, configure your Gemini API key.";
            case ANOMALY_EXPLANATION -> "Your anomaly detection is active! 🚨 Check the notification bell (🔔) for any active alerts. The AI monitors for unusual transactions automatically.";
            case WHAT_IF -> dataContext + "\n\n💡 Configure your Gemini API key for rich what-if scenario analysis!";
            default -> "I can help with spending summaries, budgets, anomalies, forecasts and what-if scenarios! Try asking one of those. For richer AI responses, configure your Gemini API key.";
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ChatSession resolveSession(String sessionIdStr, String ownerEmail) {
        if (sessionIdStr != null && !sessionIdStr.isBlank()) {
            try {
                UUID sid = UUID.fromString(sessionIdStr);
                return sessionRepo.findByIdAndOwnerEmail(sid, ownerEmail)
                    .orElseGet(() -> sessionRepo.save(new ChatSession(ownerEmail)));
            } catch (IllegalArgumentException ignored) {}
        }
        return sessionRepo.save(new ChatSession(ownerEmail));
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        try { return ((Number) val).doubleValue(); }
        catch (Exception e) { try { return Double.parseDouble(val.toString()); } catch (Exception ex) { return 0.0; } }
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        try { return ((Number) val).longValue(); }
        catch (Exception e) { try { return Long.parseLong(val.toString()); } catch (Exception ex) { return 0L; } }
    }
}
