package com.paklog.wms.wave.domain.event;

import com.paklog.domain.shared.DomainEvent;

import java.time.LocalDateTime;

/**
 * Domain event published when a wave is cancelled
 */
public class WaveCancelledEvent extends DomainEvent {

    private final String waveId;
    private final String reason;
    private final LocalDateTime cancelledAt;

    public WaveCancelledEvent(String waveId, String reason) {
        super();
        this.waveId = waveId;
        this.reason = reason;
        this.cancelledAt = LocalDateTime.now();
    }

    public String getWaveId() {
        return waveId;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
