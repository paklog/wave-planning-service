package com.paklog.wms.wave.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wave.planning.infrastructure.outbox.OutboxRepository;
import com.paklog.wave.planning.infrastructure.outbox.OutboxService;
import com.paklog.wms.wave.infrastructure.outbox.MongoOutboxRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Configuration for Outbox pattern implementation
 * Provides MongoDB-based outbox service for reliable event publishing
 */
@Configuration
public class OutboxConfig {

    @Bean
    public OutboxRepository outboxRepository(MongoTemplate mongoTemplate) {
        return new MongoOutboxRepository(mongoTemplate);
    }

    @Bean
    public OutboxService outboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        return new OutboxService(outboxRepository, objectMapper);
    }
}
