package com.paklog.wave.planning.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.CloudEventSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for transactional outbox pattern in Wave Planning Service
 * Copied from paklog-integration to eliminate compilation dependency
 */
@Configuration
@EnableScheduling
public class OutboxConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Configure ObjectMapper with Java 8 time support
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Configure Kafka producer for CloudEvents
     */
    @Bean
    @ConditionalOnProperty(name = "wave-planning.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
    public ProducerFactory<String, CloudEvent> cloudEventProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CloudEventSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Configure KafkaTemplate for CloudEvents
     */
    @Bean
    @ConditionalOnProperty(name = "wave-planning.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
    public KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplate(
            ProducerFactory<String, CloudEvent> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}
