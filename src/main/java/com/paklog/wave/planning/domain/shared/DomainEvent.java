package com.paklog.wave.planning.domain.shared;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in Wave Planning bounded context
 * Copied from paklog-domain to eliminate compilation dependency
 */
public abstract class DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
