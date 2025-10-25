package com.paklog.wms.wave.domain.valueobject;

/**
 * Wave planning strategy types
 * Based on detailed_plan.md specification
 */
public enum WaveStrategyType {
    /**
     * Group orders by time-based intervals (e.g., hourly waves)
     */
    TIME_BASED,

    /**
     * Group orders by carrier for efficient carrier pickups
     */
    CARRIER_BASED,

    /**
     * Group orders by warehouse zone for efficient picking
     */
    ZONE_BASED,

    /**
     * Group orders based on capacity constraints such as volume and weight
     */
    CAPACITY_BASED,

    /**
     * Group orders by priority (critical orders first)
     */
    PRIORITY_BASED,

    /**
     * Custom strategy implementation
     */
    CUSTOM;

    public String getDescription() {
        return switch (this) {
            case TIME_BASED -> "Time-based wave planning with configurable intervals";
            case CARRIER_BASED -> "Carrier-based wave planning for optimized shipments";
            case ZONE_BASED -> "Zone-based wave planning for efficient picking";
            case CAPACITY_BASED -> "Capacity-based wave planning to respect operational limits";
            case PRIORITY_BASED -> "Priority-based wave planning for critical orders";
            case CUSTOM -> "Custom wave planning strategy";
        };
    }
}
