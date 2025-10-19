package com.paklog.wms.wave.domain.valueobject;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object representing carrier cutoff time
 */
public class CarrierCutoff {

    private final String carrier;
    private final LocalDateTime cutoffTime;
    private final String serviceLevel;

    public CarrierCutoff(String carrier, LocalDateTime cutoffTime, String serviceLevel) {
        this.carrier = Objects.requireNonNull(carrier, "Carrier cannot be null");
        this.cutoffTime = Objects.requireNonNull(cutoffTime, "Cutoff time cannot be null");
        this.serviceLevel = serviceLevel;
    }

    public String getCarrier() {
        return carrier;
    }

    public LocalDateTime getCutoffTime() {
        return cutoffTime;
    }

    public String getServiceLevel() {
        return serviceLevel;
    }

    public boolean isBeforeCutoff(LocalDateTime time) {
        return time.isBefore(cutoffTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarrierCutoff that = (CarrierCutoff) o;
        return carrier.equals(that.carrier) &&
                cutoffTime.equals(that.cutoffTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(carrier, cutoffTime);
    }

    @Override
    public String toString() {
        return String.format("CarrierCutoff{carrier='%s', cutoff=%s, service='%s'}",
                carrier, cutoffTime, serviceLevel);
    }
}
