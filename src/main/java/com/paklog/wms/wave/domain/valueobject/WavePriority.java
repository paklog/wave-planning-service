package com.paklog.wms.wave.domain.valueobject;

/**
 * Wave priority levels
 * Uses same priority model as paklog-domain but specific to waves
 */
public enum WavePriority {
    CRITICAL(1),
    HIGH(2),
    NORMAL(3),
    LOW(4);

    private final int value;

    WavePriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public boolean isHigherThan(WavePriority other) {
        return this.value < other.value;
    }

    public boolean isLowerThan(WavePriority other) {
        return this.value > other.value;
    }

    /**
     * Convert from paklog-domain Priority
     */
    public static WavePriority fromDomainPriority(com.paklog.domain.valueobject.Priority priority) {
        return valueOf(priority.name());
    }

    /**
     * Convert to paklog-domain Priority
     */
    public com.paklog.domain.valueobject.Priority toDomainPriority() {
        return com.paklog.domain.valueobject.Priority.valueOf(this.name());
    }
}
