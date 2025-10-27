package com.paklog.wave.planning.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for writing events to the transactional outbox in Wave Planning Service
 * This should be called within the same transaction as your business logic
 * Copied from paklog-integration to eliminate compilation dependency
 */
@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Save an event to the outbox within the current transaction
     * @param aggregateId the aggregate identifier (e.g., waveId, orderId)
     * @param eventType the CloudEvents type (e.g., com.paklog.wms.wave-planning.wave.released.v1)
     * @param event the event object to serialize
     * @return the saved outbox event
     */
    @Transactional
    public OutboxEvent saveEvent(String aggregateId, String eventType, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payload)
                    .status(OutboxStatus.PENDING)
                    .build();

            OutboxEvent saved = outboxRepository.save(outboxEvent);
            log.debug("Saved event to outbox: id={}, type={}, aggregateId={}",
                    saved.getId(), eventType, aggregateId);

            return saved;
        } catch (Exception e) {
            log.error("Failed to serialize and save event to outbox: type={}, aggregateId={}",
                    eventType, aggregateId, e);
            throw new OutboxException("Failed to save event to outbox", e);
        }
    }

    /**
     * Save multiple events to the outbox within the current transaction
     * @param events list of outbox events to save
     * @return list of saved events
     */
    @Transactional
    public List<OutboxEvent> saveEvents(List<OutboxEvent> events) {
        List<OutboxEvent> saved = outboxRepository.saveAll(events);
        log.debug("Saved {} events to outbox", saved.size());
        return saved;
    }

    /**
     * Mark an event as published
     * @param eventId the event identifier
     */
    @Transactional
    public void markAsPublished(String eventId) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.markAsPublished();
            outboxRepository.save(event);
            log.debug("Marked event as published: id={}, type={}", eventId, event.getEventType());
        });
    }

    /**
     * Mark an event as failed
     * @param eventId the event identifier
     */
    @Transactional
    public void markAsFailed(String eventId) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(OutboxStatus.FAILED);
            event.incrementRetry();
            outboxRepository.save(event);
            log.warn("Marked event as failed: id={}, type={}, retryCount={}",
                    eventId, event.getEventType(), event.getRetryCount());
        });
    }

    /**
     * Get events for a specific aggregate
     * @param aggregateId the aggregate identifier
     * @return list of events for the aggregate
     */
    public List<OutboxEvent> getEventsByAggregate(String aggregateId) {
        return outboxRepository.findByAggregateIdOrderByCreatedAtDesc(aggregateId);
    }

    /**
     * Get count of pending events
     * @return count of pending events
     */
    public long getPendingEventCount() {
        return outboxRepository.countByStatus(OutboxStatus.PENDING);
    }

    /**
     * Get count of failed events
     * @return count of failed events
     */
    public long getFailedEventCount() {
        return outboxRepository.countByStatus(OutboxStatus.FAILED);
    }
}
