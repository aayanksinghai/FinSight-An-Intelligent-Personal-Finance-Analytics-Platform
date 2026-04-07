package com.finsight.chat.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Calls transaction-service on behalf of an authenticated user.
 * The JWT token is forwarded so authorisation is upheld end-to-end.
 */
@Component
public class TransactionClient {

    private final WebClient webClient;

    public TransactionClient(@Qualifier("transactionWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /** Get spending summary (total debits/credits) for a period. */
    public List<Map<String, Object>> getSummary(String bearerToken, Instant from, Instant to) {
        return webClient.get()
            .uri(uri -> uri.path("/api/transactions/summary")
                .queryParam("from", from.toString())
                .queryParam("to", to.toString())
                .build())
            .header("Authorization", bearerToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
            .block();
    }

    /** Get spend breakdown by category for a period. */
    public List<Map<String, Object>> getCategories(String bearerToken, Instant from, Instant to) {
        return webClient.get()
            .uri(uri -> uri.path("/api/transactions/categories")
                .queryParam("from", from.toString())
                .queryParam("to", to.toString())
                .build())
            .header("Authorization", bearerToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
            .block();
    }

    /** Get last N months of category spend for forecast and what-if. */
    public List<Map<String, Object>> getRecentCategories(String bearerToken, int months) {
        Instant to = Instant.now();
        Instant from = to.minus(months * 30L, ChronoUnit.DAYS);
        return getCategories(bearerToken, from, to);
    }
}
