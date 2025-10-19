package com.paklog.wms.wave.domain.event;

import com.paklog.domain.shared.DomainEvent;

import java.time.LocalDateTime;

/**
 * Domain event published when a wave is completed
 */
public class WaveCompletedEvent extends DomainEvent {

    private final String waveId;
    private final LocalDateTime completedAt;
    private final Integer totalOrders;
    private final Integer completedOrders;
    private final Double pickAccuracy;

    public WaveCompletedEvent(String waveId, Integer totalOrders, Integer completedOrders,
                             Double pickAccuracy) {
        super();
        this.waveId = waveId;
        this.completedAt = LocalDateTime.now();
        this.totalOrders = totalOrders;
        this.completedOrders = completedOrders;
        this.pickAccuracy = pickAccuracy;
    }

    public String getWaveId() {
        return waveId;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public Integer getCompletedOrders() {
        return completedOrders;
    }

    public Double getPickAccuracy() {
        return pickAccuracy;
    }
}
