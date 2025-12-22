package com.paklog.wms.wave.domain.aggregate;

import com.paklog.wave.planning.domain.shared.AggregateRoot;
import com.paklog.wave.planning.domain.shared.DomainEvent;
import com.paklog.wms.wave.domain.entity.WaveMetrics;
import com.paklog.wms.wave.domain.event.WaveCancelledEvent;
import com.paklog.wms.wave.domain.event.WaveCompletedEvent;
import com.paklog.wms.wave.domain.event.WavePlannedEvent;
import com.paklog.wms.wave.domain.event.WaveReleasedEvent;
import com.paklog.wms.wave.domain.event.InventoryAllocationRequestedEvent;
import com.paklog.wms.wave.domain.valueobject.WaveId;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStatus;
import com.paklog.wms.wave.domain.valueobject.WaveStrategy;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Wave Aggregate Root
 * Represents a batch of orders to be picked together
 * Implements state machine pattern for wave lifecycle
 */
@AggregateRoot
@Document(collection = "waves")
public class Wave {

    @Id
    private String waveId;

    private WaveStatus status;
    private List<String> orderIds;
    private WavePriority priority;
    private WaveStrategy strategy;
    private String warehouseId;
    private String assignedZone;
    private LocalDateTime plannedReleaseTime;
    private LocalDateTime actualReleaseTime;
    private LocalDateTime completedAt;
    private WaveMetrics metrics;
    private boolean inventoryAllocated;

    @Version
    private Long version; // Optimistic locking

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // Default constructor for MongoDB
    public Wave() {
        this.status = WaveStatus.PLANNED;
        this.orderIds = new ArrayList<>();
        this.metrics = new WaveMetrics();
        this.inventoryAllocated = false;
    }

    /**
     * Plan a new wave with orders and strategy
     */
    public void plan(List<String> orderIds, WaveStrategy strategy, String warehouseId,
                    WavePriority priority, LocalDateTime plannedReleaseTime) {
        validateOrders(orderIds);
        Objects.requireNonNull(strategy, "Strategy cannot be null");
        Objects.requireNonNull(warehouseId, "Warehouse ID cannot be null");

        this.orderIds = new ArrayList<>(orderIds);
        this.strategy = strategy;
        this.warehouseId = warehouseId;
        this.priority = priority != null ? priority : WavePriority.NORMAL;
        this.plannedReleaseTime = plannedReleaseTime;
        this.status = WaveStatus.PLANNED;

        // Initialize metrics
        this.metrics.setTotalOrders(orderIds.size());

        registerEvent(new WavePlannedEvent(
            this.waveId,
            this.orderIds,
            this.warehouseId,
            this.strategy.getType(),
            this.priority,
            this.plannedReleaseTime
        ));
    }

    /**
     * Release wave for execution
     * Triggers task generation in WES
     *
     * @param skuQuantities Pre-calculated SKU quantities from Order Management Service
     */
    public void release(Map<String, Integer> skuQuantities) {
        ensureStatus(WaveStatus.PLANNED);
        ensureInventoryAllocated();

        if (assignedZone == null) {
            throw new IllegalStateException("Wave must have an assigned zone before release");
        }

        this.status = WaveStatus.RELEASED;
        this.actualReleaseTime = LocalDateTime.now();
        this.metrics.recordPickStart();

        // Publish wave released event
        registerEvent(new WaveReleasedEvent(
            this.waveId,
            this.orderIds,
            this.warehouseId,
            this.assignedZone,
            this.priority
        ));

        // ⭐ HARD ALLOCATION: Request final inventory allocation at wave release
        // This locks inventory for immediate physical picking (part of hybrid allocation strategy)
        // SKU quantities are calculated by WaveSkuCalculationService from Order Management
        Map<String, Integer> quantities = skuQuantities != null ? skuQuantities : new HashMap<>();
        registerEvent(new InventoryAllocationRequestedEvent(
            this.waveId,
            this.orderIds,
            this.warehouseId,
            quantities
        ));
    }

    /**
     * Overload for backwards compatibility - calculates empty SKU map
     * @deprecated Use release(Map<String, Integer> skuQuantities) instead
     */
    @Deprecated
    public void release() {
        release(new HashMap<>());
    }

    /**
     * Start wave execution
     */
    public void startExecution() {
        ensureStatus(WaveStatus.RELEASED);

        this.status = WaveStatus.IN_PROGRESS;
    }

    /**
     * Complete the wave
     */
    public void complete() {
        ensureStatus(WaveStatus.IN_PROGRESS);

        this.status = WaveStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.metrics.recordPickCompletion();
        this.metrics.calculateEfficiency();

        registerEvent(new WaveCompletedEvent(
            this.waveId,
            this.metrics.getTotalOrders(),
            this.metrics.getCompletedOrders(),
            this.metrics.getPickAccuracy()
        ));
    }

    /**
     * Cancel the wave with a reason
     */
    public void cancel(String reason) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot cancel a " + status + " wave");
        }

        Objects.requireNonNull(reason, "Cancellation reason is required");

        this.status = WaveStatus.CANCELLED;

        registerEvent(new WaveCancelledEvent(this.waveId, reason));
    }

    /**
     * Assign zone to wave
     */
    public void assignZone(String zone) {
        Objects.requireNonNull(zone, "Zone cannot be null");
        if (this.status != WaveStatus.PLANNED) {
            throw new IllegalStateException("Can only assign zone to planned waves");
        }
        this.assignedZone = zone;
    }

    /**
     * Mark inventory as allocated for this wave
     */
    public void markInventoryAllocated() {
        if (this.status != WaveStatus.PLANNED) {
            throw new IllegalStateException("Can only allocate inventory for planned waves");
        }
        this.inventoryAllocated = true;
    }

    /**
     * Add orders to the wave
     */
    public void addOrders(List<String> newOrderIds) {
        if (this.status != WaveStatus.PLANNED) {
            throw new IllegalStateException("Can only add orders to planned waves");
        }

        validateOrders(newOrderIds);

        // Check strategy capacity
        int totalOrders = this.orderIds.size() + newOrderIds.size();
        if (strategy.getMaxOrders() != null && totalOrders > strategy.getMaxOrders()) {
            throw new IllegalStateException("Adding orders would exceed max wave capacity");
        }

        this.orderIds.addAll(newOrderIds);
        this.metrics.setTotalOrders(this.orderIds.size());
    }

    /**
     * Reorder existing orders without changing membership.
     */
    public void reorderOrders(List<String> newOrderSequence) {
        Objects.requireNonNull(newOrderSequence, "Order sequence cannot be null");
        if (this.status != WaveStatus.PLANNED) {
            throw new IllegalStateException("Can only reorder orders for planned waves");
        }
        if (newOrderSequence.size() != this.orderIds.size()
                || !this.orderIds.containsAll(newOrderSequence)) {
            throw new IllegalArgumentException("New sequence must contain the same orders");
        }
        this.orderIds = new ArrayList<>(newOrderSequence);
    }

    /**
     * Remove orders from the wave
     */
    public void removeOrders(List<String> orderIdsToRemove) {
        if (this.status != WaveStatus.PLANNED) {
            throw new IllegalStateException("Can only remove orders from planned waves");
        }

        this.orderIds.removeAll(orderIdsToRemove);
        this.metrics.setTotalOrders(this.orderIds.size());
    }

    // Validation methods
    private void validateOrders(List<String> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("Wave must contain at least one order");
        }
    }

    private void ensureStatus(WaveStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new IllegalStateException(
                String.format("Wave must be in %s status, but is %s", expectedStatus, this.status)
            );
        }
    }

    private void ensureInventoryAllocated() {
        if (!this.inventoryAllocated) {
            throw new IllegalStateException("Cannot release wave without inventory allocation");
        }
    }

    // Event management
    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // Getters and setters
    public String getWaveId() {
        return waveId;
    }

    public void setWaveId(String waveId) {
        this.waveId = waveId;
    }

    public WaveStatus getStatus() {
        return status;
    }

    public List<String> getOrderIds() {
        return new ArrayList<>(orderIds);
    }

    public WavePriority getPriority() {
        return priority;
    }

    public void setPriority(WavePriority priority) {
        this.priority = priority;
    }

    public WaveStrategy getStrategy() {
        return strategy;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getAssignedZone() {
        return assignedZone;
    }

    public LocalDateTime getPlannedReleaseTime() {
        return plannedReleaseTime;
    }

    public void setPlannedReleaseTime(LocalDateTime plannedReleaseTime) {
        this.plannedReleaseTime = plannedReleaseTime;
    }

    public LocalDateTime getActualReleaseTime() {
        return actualReleaseTime;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public WaveMetrics getMetrics() {
        return metrics;
    }

    public boolean isInventoryAllocated() {
        return inventoryAllocated;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wave wave = (Wave) o;
        return Objects.equals(waveId, wave.waveId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(waveId);
    }

    @Override
    public String toString() {
        return "Wave{" +
                "waveId='" + waveId + '\'' +
                ", status=" + status +
                ", orderCount=" + orderIds.size() +
                ", priority=" + priority +
                ", warehouseId='" + warehouseId + '\'' +
                '}';
    }
}
