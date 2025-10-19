package com.paklog.wms.wave.domain.event;

import com.paklog.domain.shared.DomainEvent;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain event published when a wave is planned
 */
public class WavePlannedEvent extends DomainEvent {

    private final String waveId;
    private final List<String> orderIds;
    private final String warehouseId;
    private final WaveStrategyType strategy;
    private final WavePriority priority;
    private final LocalDateTime plannedReleaseTime;

    public WavePlannedEvent(String waveId, List<String> orderIds, String warehouseId,
                           WaveStrategyType strategy, WavePriority priority,
                           LocalDateTime plannedReleaseTime) {
        super();
        this.waveId = waveId;
        this.orderIds = orderIds;
        this.warehouseId = warehouseId;
        this.strategy = strategy;
        this.priority = priority;
        this.plannedReleaseTime = plannedReleaseTime;
    }

    public String getWaveId() {
        return waveId;
    }

    public List<String> getOrderIds() {
        return orderIds;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public WaveStrategyType getStrategy() {
        return strategy;
    }

    public WavePriority getPriority() {
        return priority;
    }

    public LocalDateTime getPlannedReleaseTime() {
        return plannedReleaseTime;
    }
}
