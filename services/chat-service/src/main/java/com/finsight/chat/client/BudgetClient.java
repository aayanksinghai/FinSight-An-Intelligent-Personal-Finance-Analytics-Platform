package com.finsight.chat.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Component
public class BudgetClient {

    private final WebClient webClient;

    public BudgetClient(@Qualifier("budgetWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /** Get all budgets for the current month. */
    public List<Map<String, Object>> getBudgetsForMonth(String bearerToken, String monthYear) {
        return webClient.get()
            .uri(uri -> uri.path("/api/budgets")
                .queryParam("monthYear", monthYear)
                .build())
            .header("Authorization", bearerToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
            .block();
    }

    /** Convenience: get budgets for the current calendar month. */
    public List<Map<String, Object>> getCurrentMonthBudgets(String bearerToken) {
        String monthYear = YearMonth.now().toString(); // e.g. "2026-04"
        return getBudgetsForMonth(bearerToken, monthYear);
    }
}
