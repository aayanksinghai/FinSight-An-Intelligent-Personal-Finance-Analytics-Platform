package com.finsight.transaction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Exponential backoff for retries: 1s, 2s, 4s, 8s, 16s (max 5 attempts)
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(5);
        
        factory.setCommonErrorHandler(new DefaultErrorHandler(backOff));

        return factory;
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic transactionsCreatedTopic() {
        return org.springframework.kafka.config.TopicBuilder.name("transactions.created")
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic transactionsCategorizedTopic() {
        return org.springframework.kafka.config.TopicBuilder.name("transactions.categorized")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
