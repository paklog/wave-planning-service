package com.paklog.wave.planning.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Polling-based relay that reads from outbox and publishes to Kafka for Wave Planning Service
 * Provides at-least-once delivery guarantee
 * Copied from paklog-integration to eliminate compilation dependency
 */
@Component
@ConditionalOnProperty(
    name = "wave-planning.outbox.relay.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${wave-planning.outbox.relay.batch-size:100}")
    private int batchSize;

    @Value("${wave-planning.outbox.relay.max-retries:3}")
    private int maxRetries;

    @Value("${wave-planning.kafka.topic:wave-planning-events}")
    private String kafkaTopic;

    @Value("${wave-planning.outbox.event-source:paklog://wave-planning-service}")
    private String eventSource;

    public OutboxRelay(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, CloudEvent> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Poll outbox and publish pending events to Kafka
     * Runs every 5 seconds by default
     */
    @Scheduled(fixedDelayString = "${wave-planning.outbox.relay.poll-interval:5000}")
    @Transactional
    public void relayPendingEvents() {
        try {
            // Get pending events
            List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents(batchSize);

            if (pendingEvents.isEmpty()) {
                return;
            }

            log.debug("Relaying {} pending events", pendingEvents.size());

            // Publish each event
            for (OutboxEvent outboxEvent : pendingEvents) {
                publishEvent(outboxEvent);
            }

            // Also retry failed events
            List<OutboxEvent> failedEvents = outboxRepository.findFailedEventsForRetry(maxRetries, batchSize);
            if (!failedEvents.isEmpty()) {
                log.info("Retrying {} failed events", failedEvents.size());
                for (OutboxEvent outboxEvent : failedEvents) {
                    publishEvent(outboxEvent);
                }
            }

        } catch (Exception e) {
            log.error("Error during outbox relay execution", e);
        }
    }

    /**
     * Publish a single event to Kafka
     */
    private void publishEvent(OutboxEvent outboxEvent) {
        try {
            // Build CloudEvent wrapper
            CloudEvent cloudEvent = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withType(outboxEvent.getEventType())
                    .withSource(URI.create(eventSource))
                    .withData("application/json", outboxEvent.getPayload().getBytes())
                    .withTime(OffsetDateTime.now())
                    .withExtension("aggregateId", outboxEvent.getAggregateId())
                    .withExtension("outboxId", outboxEvent.getId())
                    .build();

            // Send to Kafka asynchronously
            CompletableFuture<Void> future = kafkaTemplate
                    .send(kafkaTopic, outboxEvent.getAggregateId(), cloudEvent)
                    .thenAccept(result -> {
                        // Mark as published on success
                        markAsPublished(outboxEvent.getId());
                        log.debug("Successfully published event: id={}, type={}",
                                outboxEvent.getId(), outboxEvent.getEventType());
                    })
                    .exceptionally(ex -> {
                        // Mark as failed on error
                        markAsFailed(outboxEvent.getId());
                        log.error("Failed to publish event: id={}, type={}",
                                outboxEvent.getId(), outboxEvent.getEventType(), ex);
                        return null;
                    });

        } catch (Exception e) {
            markAsFailed(outboxEvent.getId());
            log.error("Error building CloudEvent for outbox event: id={}", outboxEvent.getId(), e);
        }
    }

    /**
     * Mark event as published (runs in separate transaction)
     */
    @Transactional
    private void markAsPublished(String eventId) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.markAsPublished();
            outboxRepository.save(event);
        });
    }

    /**
     * Mark event as failed (runs in separate transaction)
     */
    @Transactional
    private void markAsFailed(String eventId) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(OutboxStatus.FAILED);
            event.incrementRetry();
            outboxRepository.save(event);
        });
    }
}
