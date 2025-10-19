package com.paklog.wms.wave.domain.valueobject;

/**
 * Wave lifecycle status
 */
public enum WaveStatus {
    PLANNED,
    RELEASED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(WaveStatus newStatus) {
        return switch (this) {
            case PLANNED -> newStatus == RELEASED || newStatus == CANCELLED;
            case RELEASED -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == COMPLETED || newStatus == CANCELLED;
            case COMPLETED -> false;
            case CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean isActive() {
        return this == RELEASED || this == IN_PROGRESS;
    }
}
