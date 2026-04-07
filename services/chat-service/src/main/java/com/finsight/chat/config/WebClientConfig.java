package com.finsight.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${services.transaction-service-url}")
    private String transactionServiceUrl;

    @Value("${services.budget-service-url}")
    private String budgetServiceUrl;

    @Value("${services.notification-service-url}")
    private String notificationServiceUrl;

    @Bean("transactionWebClient")
    public WebClient transactionWebClient() {
        return WebClient.builder().baseUrl(transactionServiceUrl).build();
    }

    @Bean("budgetWebClient")
    public WebClient budgetWebClient() {
        return WebClient.builder().baseUrl(budgetServiceUrl).build();
    }

    @Bean("notificationWebClient")
    public WebClient notificationWebClient() {
        return WebClient.builder().baseUrl(notificationServiceUrl).build();
    }
}
