package com.finsight.chat.intent;

import com.finsight.chat.model.ChatIntent;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Rule-based NLP intent classifier.
 * No external AI API required — uses keyword and regex matching on the user's query.
 */
@Component
public class IntentClassifier {

    // ── SPENDING_SUMMARY ──────────────────────────────────────────────────────
    private static final Pattern SPENDING_SUMMARY = Pattern.compile(
        "(?i)(how much|total|spent|spending|expenses|expenditure).*(last month|this month|march|april|may|june|july|august|september|october|november|december|january|february|week|today|yesterday|period|time)", Pattern.CASE_INSENSITIVE
    );

    // ── CATEGORY_COMPARISON ───────────────────────────────────────────────────
    private static final Pattern CATEGORY_COMPARISON = Pattern.compile(
        "(?i)(compare|comparison|versus|vs\\.?|breakdown|categories|category|most|top|highest|biggest).*(spend|spending|expense|cost)", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CATEGORY_COMPARISON2 = Pattern.compile(
        "(?i)(what|which) (category|categories|area).*(spend|cost|most|highest)", Pattern.CASE_INSENSITIVE
    );

    // ── ANOMALY_EXPLANATION ───────────────────────────────────────────────────
    private static final Pattern ANOMALY = Pattern.compile(
        "(?i)(anomal|flag|unusual|detect|suspicious|irregular|alert|weird|strange|unexpected).*(transaction|spending|spend|payment|charge)", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ANOMALY2 = Pattern.compile(
        "(?i)(why|what).*(flag|flagged|detected|anomal|alert)", Pattern.CASE_INSENSITIVE
    );

    // ── BUDGET_STATUS ─────────────────────────────────────────────────────────
    private static final Pattern BUDGET = Pattern.compile(
        "(?i)(budget|over budget|under budget|limit|allowance|remaining|left|quota).*(status|check|month|category)?", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BUDGET2 = Pattern.compile(
        "(?i)(am i|are we|have i).*(over|under|within).*(budget|limit)", Pattern.CASE_INSENSITIVE
    );

    // ── FORECAST_INQUIRY ──────────────────────────────────────────────────────
    private static final Pattern FORECAST = Pattern.compile(
        "(?i)(forecast|predict|projection|estimate|next month|future|trend|will i|expect|anticipated)", Pattern.CASE_INSENSITIVE
    );

    // ── WHAT_IF ───────────────────────────────────────────────────────────────
    private static final Pattern WHAT_IF = Pattern.compile(
        "(?i)(what if|if i|scenario|reduce|cut|decrease|save|stop|savings|hypothetical)", Pattern.CASE_INSENSITIVE
    );

    public ChatIntent classify(String query) {
        if (query == null || query.isBlank()) return ChatIntent.UNKNOWN;
        String q = query.trim();

        if (ANOMALY.matcher(q).find() || ANOMALY2.matcher(q).find()) return ChatIntent.ANOMALY_EXPLANATION;
        if (WHAT_IF.matcher(q).find()) return ChatIntent.WHAT_IF;
        if (FORECAST.matcher(q).find()) return ChatIntent.FORECAST_INQUIRY;
        if (CATEGORY_COMPARISON.matcher(q).find() || CATEGORY_COMPARISON2.matcher(q).find()) return ChatIntent.CATEGORY_COMPARISON;
        if (BUDGET.matcher(q).find() || BUDGET2.matcher(q).find()) return ChatIntent.BUDGET_STATUS;
        if (SPENDING_SUMMARY.matcher(q).find()) return ChatIntent.SPENDING_SUMMARY;

        // Fallback: simple spending summary if mentions money/amount/spend
        if (q.matches("(?i).*\\b(spend|spent|expenses|cost|amount|transaction|payment)\\b.*")) {
            return ChatIntent.SPENDING_SUMMARY;
        }

        return ChatIntent.UNKNOWN;
    }
}
