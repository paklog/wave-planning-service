package com.paklog.wms.wave.domain.valueobject;

/**
 * Wave priority levels
 * Independent priority model for Wave Planning bounded context
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
}
