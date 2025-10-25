package com.paklog.wms.wave.domain.entity;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Entity representing wave performance metrics
 * Tracks planning and execution metrics for a wave
 */
public class WaveMetrics {

    private Double plannedPickTime;
    private Double actualPickTime;
    private Double pickAccuracy;
    private Double laborEfficiency;
    private Double orderFillRate;
    private Integer totalOrders;
    private Integer completedOrders;
    private Integer totalLines;
    private Integer completedLines;
    private Integer totalUnits;
    private BigDecimal totalVolume;
    private BigDecimal totalWeight;
    private Integer estimatedPickers;
    private LocalDateTime estimatedCompletionTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public WaveMetrics() {
        // Default constructor for MongoDB
        this.pickAccuracy = 100.0;
        this.laborEfficiency = 0.0;
        this.orderFillRate = 0.0;
        this.totalOrders = 0;
        this.completedOrders = 0;
        this.totalLines = 0;
        this.completedLines = 0;
        this.totalUnits = 0;
        this.totalVolume = BigDecimal.ZERO;
        this.totalWeight = BigDecimal.ZERO;
        this.estimatedPickers = 0;
        this.estimatedCompletionTime = null;
    }

    public void recordPickStart() {
        this.startTime = LocalDateTime.now();
    }

    public void recordPickCompletion() {
        this.endTime = LocalDateTime.now();
        if (startTime != null) {
            this.actualPickTime = (double) Duration.between(startTime, endTime).toMinutes();
        }
    }

    public void updateOrderCompletion(int completed) {
        this.completedOrders = completed;
        if (totalOrders > 0) {
            this.orderFillRate = (double) completedOrders / totalOrders * 100.0;
        }
    }

    public void updateLineCompletion(int completed) {
        this.completedLines = completed;
    }

    public void calculateEfficiency() {
        if (plannedPickTime != null && actualPickTime != null && actualPickTime > 0) {
            this.laborEfficiency = (plannedPickTime / actualPickTime) * 100.0;
        }
    }

    public boolean isComplete() {
        return completedOrders != null &&
               totalOrders != null &&
               completedOrders.equals(totalOrders);
    }

    // Getters and setters
    public Double getPlannedPickTime() {
        return plannedPickTime;
    }

    public void setPlannedPickTime(Double plannedPickTime) {
        this.plannedPickTime = plannedPickTime;
    }

    public Double getActualPickTime() {
        return actualPickTime;
    }

    public void setActualPickTime(Double actualPickTime) {
        this.actualPickTime = actualPickTime;
    }

    public Double getPickAccuracy() {
        return pickAccuracy;
    }

    public void setPickAccuracy(Double pickAccuracy) {
        this.pickAccuracy = pickAccuracy;
    }

    public Double getLaborEfficiency() {
        return laborEfficiency;
    }

    public void setLaborEfficiency(Double laborEfficiency) {
        this.laborEfficiency = laborEfficiency;
    }

    public Double getOrderFillRate() {
        return orderFillRate;
    }

    public void setOrderFillRate(Double orderFillRate) {
        this.orderFillRate = orderFillRate;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Integer getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(Integer completedOrders) {
        this.completedOrders = completedOrders;
    }

    public Integer getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(Integer totalLines) {
        this.totalLines = totalLines;
    }

    public Integer getCompletedLines() {
        return completedLines;
    }

    public void setCompletedLines(Integer completedLines) {
        this.completedLines = completedLines;
    }

    public Integer getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(Integer totalUnits) {
        this.totalUnits = totalUnits;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(BigDecimal totalVolume) {
        this.totalVolume = totalVolume;
    }

    public BigDecimal getTotalWeight() {
        return totalWeight;
    }

    public void setTotalWeight(BigDecimal totalWeight) {
        this.totalWeight = totalWeight;
    }

    public Integer getEstimatedPickers() {
        return estimatedPickers;
    }

    public void setEstimatedPickers(Integer estimatedPickers) {
        this.estimatedPickers = estimatedPickers;
    }

    public LocalDateTime getEstimatedCompletionTime() {
        return estimatedCompletionTime;
    }

    public void setEstimatedCompletionTime(LocalDateTime estimatedCompletionTime) {
        this.estimatedCompletionTime = estimatedCompletionTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
