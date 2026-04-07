package com.finsight.chat.service;

import com.finsight.chat.client.BudgetClient;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final IntentClassifier intentClassifier;
    private final TransactionClient transactionClient;
    private final BudgetClient budgetClient;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatRatingRepository ratingRepo;

    public ChatService(IntentClassifier intentClassifier,
                       TransactionClient transactionClient,
                       BudgetClient budgetClient,
                       ChatSessionRepository sessionRepo,
                       ChatMessageRepository messageRepo,
                       ChatRatingRepository ratingRepo) {
        this.intentClassifier = intentClassifier;
        this.transactionClient = transactionClient;
        this.budgetClient = budgetClient;
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

        // 4. Generate data-grounded response
        String responseText = generateResponse(intent, request.content(), bearerToken, ownerEmail);

        // 5. Persist assistant message
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
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

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

    private String generateResponse(ChatIntent intent, String query, String bearerToken, String ownerEmail) {
        try {
            return switch (intent) {
                case SPENDING_SUMMARY    -> handleSpendingSummary(bearerToken);
                case CATEGORY_COMPARISON -> handleCategoryComparison(bearerToken);
                case ANOMALY_EXPLANATION -> handleAnomalyExplanation();
                case FORECAST_INQUIRY    -> handleForecast(bearerToken);
                case BUDGET_STATUS       -> handleBudgetStatus(bearerToken);
                case WHAT_IF             -> handleWhatIf(query, bearerToken);
                default                  -> handleUnknown();
            };
        } catch (Exception e) {
            return "I'm sorry, I encountered an issue fetching your financial data. Please try again in a moment. (Error: " + e.getMessage() + ")";
        }
    }

    // ── Intent Handlers ──────────────────────────────────────────────────────

    private String handleSpendingSummary(String bearerToken) {
        Instant to = Instant.now();
        Instant from = to.minus(30, ChronoUnit.DAYS);
        List<Map<String, Object>> summary = transactionClient.getSummary(bearerToken, from, to);

        if (summary == null || summary.isEmpty()) {
            return "📊 I couldn't find any transactions in the last 30 days. Try uploading a bank statement first.";
        }

        StringBuilder sb = new StringBuilder("📊 **Spending Summary — Last 30 Days**\n\n");
        double totalDebit = 0, totalCredit = 0;
        for (Map<String, Object> row : summary) {
            String type = String.valueOf(row.getOrDefault("type", "UNKNOWN"));
            double amount = toDouble(row.get("totalAmount"));
            if ("DEBIT".equalsIgnoreCase(type)) { totalDebit = amount; sb.append("💸 **Total Spent:** ₹").append(String.format("%.2f", amount)).append("\n"); }
            if ("CREDIT".equalsIgnoreCase(type)) { totalCredit = amount; sb.append("💰 **Total Income:** ₹").append(String.format("%.2f", amount)).append("\n"); }
        }
        double net = totalCredit - totalDebit;
        sb.append("\n📈 **Net Cash Flow:** ₹").append(String.format("%.2f", net));
        if (net < 0) sb.append(" ⚠️ You spent more than you earned this period.");
        else sb.append(" ✅ You were net positive this period.");
        return sb.toString();
    }

    private String handleCategoryComparison(String bearerToken) {
        Instant to = Instant.now();
        Instant from = to.minus(30, ChronoUnit.DAYS);
        List<Map<String, Object>> cats = transactionClient.getCategories(bearerToken, from, to);

        if (cats == null || cats.isEmpty()) {
            return "📂 I couldn't find categorized transactions. Make sure your statement has been uploaded and processed.";
        }

        cats.sort((a, b) -> Double.compare(toDouble(b.get("totalAmount")), toDouble(a.get("totalAmount"))));
        StringBuilder sb = new StringBuilder("📂 **Category Breakdown — Last 30 Days**\n\n");
        double total = cats.stream().mapToDouble(c -> toDouble(c.get("totalAmount"))).sum();
        for (int i = 0; i < Math.min(cats.size(), 8); i++) {
            Map<String, Object> cat = cats.get(i);
            double amt = toDouble(cat.get("totalAmount"));
            double pct = total > 0 ? (amt / total * 100) : 0;
            String name = String.valueOf(cat.getOrDefault("category", "Other"));
            sb.append(String.format("• **%s**: ₹%.2f (%.1f%%)\n", name, amt, pct));
        }
        if (!cats.isEmpty()) {
            String topCat = String.valueOf(cats.get(0).getOrDefault("category", "Unknown"));
            sb.append("\n🏆 Your highest spending category is **").append(topCat).append("**.");
        }
        return sb.toString();
    }

    private String handleAnomalyExplanation() {
        return """
            🚨 **Anomaly Detection Status**

            Your anomaly detection service monitors your transaction patterns in real-time. Here's how it works:

            • **What triggers an anomaly**: Transactions that deviate significantly from your historical spending patterns — such as unusually large amounts, transactions in unknown categories, or late-night activity.
            • **How to view flagged alerts**: Check your 🔔 notification inbox for active anomaly alerts.
            • **What to do**: If you recognize the transaction, no action needed. If it's unfamiliar, contact your bank immediately.

            💡 Tip: Consistent uploads help the anomaly model learn your normal patterns more accurately over time.
            """;
    }

    private String handleForecast(String bearerToken) {
        List<Map<String, Object>> recent = transactionClient.getRecentCategories(bearerToken, 2);
        if (recent == null || recent.isEmpty()) {
            return "🔮 Not enough historical data to generate a forecast. Upload at least 2 months of statements for accurate predictions.";
        }

        double totalThisMonth = recent.stream().mapToDouble(c -> toDouble(c.get("totalAmount"))).sum();
        // Simple linear projection: assume 10% variance
        double forecastMin = totalThisMonth * 0.90;
        double forecastMax = totalThisMonth * 1.10;

        return String.format("""
            🔮 **Spending Forecast — Next Month**

            Based on your recent transactions, I estimate your next month's spending will be:

            📉 **Conservative estimate**: ₹%.2f
            📈 **High estimate**: ₹%.2f
            🎯 **Central projection**: ₹%.2f

            This is based on your recent 2-month spending average. For more accurate forecasts, upload more historical statements.

            💡 **Tip**: Setting budgets for your top categories will help you stay closer to the conservative estimate.
            """, forecastMin, forecastMax, totalThisMonth);
    }

    private String handleBudgetStatus(String bearerToken) {
        List<Map<String, Object>> budgets = budgetClient.getCurrentMonthBudgets(bearerToken);
        if (budgets == null || budgets.isEmpty()) {
            return "📋 You have no budgets set for this month. Head to the **Budgets** page to create category limits!";
        }

        StringBuilder sb = new StringBuilder("📋 **Budget Status — This Month**\n\n");
        int overCount = 0;
        for (Map<String, Object> budget : budgets) {
            String cat = String.valueOf(budget.getOrDefault("categoryName", "Unknown"));
            double limit = toDouble(budget.get("limitAmount"));
            double spent = toDouble(budget.get("spentAmount"));
            double remaining = limit - spent;
            double pct = limit > 0 ? (spent / limit * 100) : 0;
            String status = pct >= 100 ? "🔴 OVER" : pct >= 80 ? "🟠 WARNING" : "🟢 OK";
            if (pct >= 100) overCount++;
            sb.append(String.format("• **%s**: ₹%.0f / ₹%.0f (%.0f%%) %s\n", cat, spent, limit, pct, status));
        }
        if (overCount > 0) {
            sb.append(String.format("\n⚠️ You are over budget in **%d** category(s). Consider reviewing your spending.", overCount));
        } else {
            sb.append("\n✅ All budgets are within limits. Great job!");
        }
        return sb.toString();
    }

    private String handleWhatIf(String query, String bearerToken) {
        List<Map<String, Object>> cats = transactionClient.getRecentCategories(bearerToken, 1);
        if (cats == null || cats.isEmpty()) {
            return "🤔 I need some transaction history to run a what-if scenario. Please upload a bank statement first.";
        }

        double totalSpend = cats.stream().mapToDouble(c -> toDouble(c.get("totalAmount"))).sum();
        // Extract reduction % from query if present
        double reductionPct = 20.0; // default 20%
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*%").matcher(query);
        if (m.find()) reductionPct = Double.parseDouble(m.group(1));

        double savings = totalSpend * (reductionPct / 100.0);
        double newSpend = totalSpend - savings;
        double annualSavings = savings * 12;

        return String.format("""
            🤔 **What-If Scenario Analysis**

            If you reduce your overall spending by **%.0f%%**:

            • 💸 Current monthly spending: ₹%.2f
            • 🎯 New monthly target: ₹%.2f
            • 💰 Monthly savings: ₹%.2f
            • 🏦 **Projected annual savings: ₹%.2f**

            💡 **How to achieve this**:
            1. Review your top spending category and set a stricter budget limit.
            2. Use the Budgets page to track progress weekly.
            3. Check anomaly alerts to avoid unexpected large expenses.
            """, reductionPct, totalSpend, newSpend, savings, annualSavings);
    }

    private String handleUnknown() {
        return """
            🤖 I'm your FinSight AI Assistant! I can help you with:

            • 📊 **Spending summaries** — "How much did I spend last month?"
            • 📂 **Category breakdown** — "What are my top spending categories?"
            • 🚨 **Anomaly explanation** — "Why was my account flagged?"
            • 🔮 **Spending forecast** — "What will I spend next month?"
            • 📋 **Budget status** — "Am I over my food budget?"
            • 🤔 **What-if scenarios** — "If I cut dining by 20%, how much do I save?"

            Try one of these questions to get started!
            """;
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        try { return ((Number) val).doubleValue(); }
        catch (Exception e) { return Double.parseDouble(val.toString()); }
    }
}
