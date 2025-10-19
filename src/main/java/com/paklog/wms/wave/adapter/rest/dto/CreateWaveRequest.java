package com.paklog.wms.wave.adapter.rest.dto;

import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API request for creating a wave
 */
public record CreateWaveRequest(
        @NotEmpty(message = "Order IDs cannot be empty")
        List<String> orderIds,

        @NotNull(message = "Strategy type is required")
        WaveStrategyType strategy,

        @NotNull(message = "Warehouse ID is required")
        String warehouseId,

        WavePriority priority,

        LocalDateTime plannedReleaseTime,

        Integer maxOrders,

        Integer maxLines,

        String timeInterval
) {
}
