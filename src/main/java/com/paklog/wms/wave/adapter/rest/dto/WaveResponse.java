package com.paklog.wms.wave.adapter.rest.dto;

import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStatus;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API response for wave data
 */
public record WaveResponse(
        String waveId,
        WaveStatus status,
        List<String> orderIds,
        Integer orderCount,
        WaveStrategyType strategy,
        WavePriority priority,
        String warehouseId,
        String assignedZone,
        LocalDateTime plannedReleaseTime,
        LocalDateTime actualReleaseTime,
        LocalDateTime completedAt,
        WaveMetricsDto metrics,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WaveResponse fromDomain(Wave wave) {
        return new WaveResponse(
                wave.getWaveId(),
                wave.getStatus(),
                wave.getOrderIds(),
                wave.getOrderIds().size(),
                wave.getStrategy().getType(),
                wave.getPriority(),
                wave.getWarehouseId(),
                wave.getAssignedZone(),
                wave.getPlannedReleaseTime(),
                wave.getActualReleaseTime(),
                wave.getCompletedAt(),
                WaveMetricsDto.fromDomain(wave.getMetrics()),
                null, // MongoDB doesn't track created/updated by default
                null
        );
    }

    public record WaveMetricsDto(
            Double plannedPickTime,
            Double actualPickTime,
            Double pickAccuracy,
            Double laborEfficiency,
            Double orderFillRate,
            Integer totalOrders,
            Integer completedOrders
    ) {
        public static WaveMetricsDto fromDomain(com.paklog.wms.wave.domain.entity.WaveMetrics metrics) {
            if (metrics == null) {
                return null;
            }
            return new WaveMetricsDto(
                    metrics.getPlannedPickTime(),
                    metrics.getActualPickTime(),
                    metrics.getPickAccuracy(),
                    metrics.getLaborEfficiency(),
                    metrics.getOrderFillRate(),
                    metrics.getTotalOrders(),
                    metrics.getCompletedOrders()
            );
        }
    }
}
