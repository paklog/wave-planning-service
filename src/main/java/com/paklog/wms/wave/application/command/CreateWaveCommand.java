package com.paklog.wms.wave.application.command;

import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Command to create a new wave
 */
public record CreateWaveCommand(
        List<String> orderIds,
        WaveStrategyType strategyType,
        String warehouseId,
        WavePriority priority,
        LocalDateTime plannedReleaseTime,
        Integer maxOrders,
        Integer maxLines,
        Duration timeInterval
) {
    public CreateWaveCommand {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new IllegalArgumentException("Order IDs cannot be null or empty");
        }
        if (strategyType == null) {
            throw new IllegalArgumentException("Strategy type cannot be null");
        }
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalArgumentException("Warehouse ID cannot be null or empty");
        

}
}
}
