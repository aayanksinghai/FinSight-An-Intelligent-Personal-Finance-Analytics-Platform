package com.finsight.ingestion.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Value("${ingestion.kafka.topic:transactions.ingested}")
    private String transactionsIngestedTopic;

    /** Ensure the topic exists at startup (idempotent if already created). */
    @Bean
    public NewTopic transactionsIngestedTopic() {
        return TopicBuilder.name(transactionsIngestedTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
