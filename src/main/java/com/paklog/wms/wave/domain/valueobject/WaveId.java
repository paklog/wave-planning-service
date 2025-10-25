package com.paklog.wms.wave.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a Wave identifier
 */
public class WaveId {

    private final String value;

    private WaveId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("WaveId cannot be null or empty");
        }
        this.value = value;
    }


    public static WaveId of(String value) {
        return new WaveId(value);
    }

    public static WaveId generate() {
        return new WaveId("WAVE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaveId waveId = (WaveId) o;
        return Objects.equals(value, waveId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
