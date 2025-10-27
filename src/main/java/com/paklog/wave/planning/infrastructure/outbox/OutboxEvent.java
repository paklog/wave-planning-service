package com.paklog.wave.planning.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an event in the transactional outbox for Wave Planning Service
 * Persisted in MongoDB to guarantee at-least-once delivery
 * Copied from paklog-integration and adapted for MongoDB
 */
public class OutboxEvent {

    private String id;
    private String aggregateId;
    private String eventType;
    private String payload;
    private OutboxStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private int retryCount;

    public OutboxEvent() {
        this.id = UUID.randomUUID().toString();
        this.status = OutboxStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public static class Builder {
        private final OutboxEvent event = new OutboxEvent();

        public Builder aggregateId(String aggregateId) {
            event.setAggregateId(aggregateId);
            return this;
        }

        public Builder eventType(String eventType) {
            event.setEventType(eventType);
            return this;
        }

        public Builder payload(String payload) {
            event.setPayload(payload);
            return this;
        }

        public Builder status(OutboxStatus status) {
            event.setStatus(status);
            return this;
        }

        public OutboxEvent build() {
            return event;
        }
    }
}
