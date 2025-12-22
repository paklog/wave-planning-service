package com.paklog.wms.wave.domain.event;

import com.paklog.wave.planning.domain.shared.DomainEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Domain event published when wave is released and requires HARD inventory allocation.
 * This represents the final commitment of inventory for physical picking.
 *
 * Published by: Wave Planning Service
 * Consumed by: Inventory Service
 *
 * Business Context:
 * - Published at wave release (HARD allocation)
 * - Supersedes any previous SOFT allocations from Order Management
 * - Locks inventory for immediate physical picking
 * - Part of hybrid allocation strategy
 */
public class InventoryAllocationRequestedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "com.paklog.wms.wave.inventory.allocation.requested";

    private final String waveId;
    private final List<String> orderIds;
    private final String warehouseId;
    private final Map<String, Integer> skuQuantities; // SKU -> Total quantity across all orders
    private final AllocationType allocationType;
    private final LocalDateTime requestedAt;

    /**
     * Creates a new inventory allocation request for a wave
     *
     * @param waveId Wave identifier
     * @param orderIds Order IDs included in the wave
     * @param warehouseId Warehouse where allocation is needed
     * @param skuQuantities Map of SKU to total quantity needed
     */
    public InventoryAllocationRequestedEvent(String waveId, List<String> orderIds,
                                            String warehouseId, Map<String, Integer> skuQuantities) {
        super();
        this.waveId = waveId;
        this.orderIds = orderIds;
        this.warehouseId = warehouseId;
        this.skuQuantities = skuQuantities;
        this.allocationType = AllocationType.HARD;  // Wave Planning always does HARD allocation
        this.requestedAt = LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
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

    public Map<String, Integer> getSkuQuantities() {
        return skuQuantities;
    }

    public AllocationType getAllocationType() {
        return allocationType;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    /**
     * Type of inventory allocation
     * SOFT: Reservation at order validation (from Order Management)
     * HARD: Final commitment at wave release (from Wave Planning) - locks for picking
     */
    public enum AllocationType {
        SOFT,   // Order Management - reservation
        HARD    // Wave Planning - committed for picking
    }
}
