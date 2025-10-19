package com.paklog.wms.wave.domain.event;

import com.paklog.domain.shared.DomainEvent;
import com.paklog.wms.wave.domain.valueobject.WavePriority;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain event published when a wave is released for execution
 * This triggers task generation in WES services
 */
public class WaveReleasedEvent extends DomainEvent {

    private final String waveId;
    private final List<String> orderIds;
    private final String warehouseId;
    private final String assignedZone;
    private final WavePriority priority;
    private final LocalDateTime releasedAt;

    public WaveReleasedEvent(String waveId, List<String> orderIds, String warehouseId,
                            String assignedZone, WavePriority priority) {
        super();
        this.waveId = waveId;
        this.orderIds = orderIds;
        this.warehouseId = warehouseId;
        this.assignedZone = assignedZone;
        this.priority = priority;
        this.releasedAt = LocalDateTime.now();
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

    public String getAssignedZone() {
        return assignedZone;
    }

    public WavePriority getPriority() {
        return priority;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }
}
