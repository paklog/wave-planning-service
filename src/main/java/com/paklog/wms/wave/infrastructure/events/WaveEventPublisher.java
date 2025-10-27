package com.paklog.wms.wave.infrastructure.events;

import com.paklog.wave.planning.domain.shared.DomainEvent;
import com.paklog.wave.planning.infrastructure.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Publisher for wave domain events
 * Uses transactional outbox pattern for reliable event publishing
 * Outbox relay will publish events to Kafka asynchronously
 */
@Component
public class WaveEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(WaveEventPublisher.class);

    private final OutboxService outboxService;

    public WaveEventPublisher(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    /**
     * Publish a list of domain events
     * @param events domain events to publish
     */
    @Transactional
    public void publishEvents(List<DomainEvent> events) {
        events.forEach(this::publishEvent);
    }

    /**
     * Publish a single domain event
     * Saves event to transactional outbox - will be published to Kafka by outbox relay
     * @param event domain event to publish
     */
    @Transactional
    public void publishEvent(DomainEvent event) {
        try {
            String aggregateId = extractAggregateId(event);
            String eventType = buildEventType(event);

            outboxService.saveEvent(aggregateId, eventType, event);

            logger.debug("Saved event to outbox: {} with aggregateId: {}", eventType, aggregateId);

        } catch (Exception e) {
            logger.error("Failed to save event to outbox: {}", event.getClass().getSimpleName(), e);
            throw new EventPublishException("Failed to save event to outbox", e);
        }
    }

    /**
     * Extract aggregate ID from domain event
     * Uses reflection to find common ID patterns
     */
    private String extractAggregateId(DomainEvent event) {
        try {
            // Try common getter patterns
            var getWaveId = event.getClass().getMethod("getWaveId");
            var waveId = getWaveId.invoke(event);
            return waveId != null ? waveId.toString() : "unknown";
        } catch (Exception e) {
            // Fallback: try getAggregateId
            try {
                var getAggregateId = event.getClass().getMethod("getAggregateId");
                var aggregateId = getAggregateId.invoke(event);
                return aggregateId != null ? aggregateId.toString() : "unknown";
            } catch (Exception ex) {
                logger.warn("Could not extract aggregate ID from event: {}", event.getClass().getSimpleName());
                return "unknown";
            }
        }
    }

    private String buildEventType(DomainEvent event) {
        String className = event.getClass().getSimpleName();
        // Convert WavePlannedEvent -> com.paklog.wms.wave.planned.v1
        String eventName = className.replace("Event", "")
                .replaceAll("([a-z])([A-Z])", "$1.$2")
                .toLowerCase();
        return "com.paklog.wms.wave." + eventName + ".v1";
    }

    public static class EventPublishException extends RuntimeException {
        public EventPublishException(String message, Throwable cause) {
            super(message, cause);
        

}
}
}
