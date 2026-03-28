package com.finsight.ingestion.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
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

    /**
     * Override the admin config to not fail at boot when Kafka is unreachable.
     * The service will still start and accept uploads; Kafka publish will fail
     * per-job if Kafka is down, which is reflected in the job's error_message.
     */
    @Bean
    public KafkaAdmin kafkaAdmin(KafkaProperties kafkaProperties) {
        KafkaAdmin admin = new KafkaAdmin(kafkaProperties.buildAdminProperties(null));
        admin.setFatalIfBrokerNotAvailable(false);
        return admin;
    }
}
