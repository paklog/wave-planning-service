package com.paklog.wms.wave.domain.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing wave capacity constraints
 */
public class WaveCapacity {

    private final int maxOrders;
    private final int maxLines;
    private final BigDecimal maxVolume;
    private final BigDecimal maxWeight;
    private final int maxPickers;

    private WaveCapacity(Builder builder) {
        this.maxOrders = builder.maxOrders;
        this.maxLines = builder.maxLines;
        this.maxVolume = builder.maxVolume;
        this.maxWeight = builder.maxWeight;
        this.maxPickers = builder.maxPickers;
    }



    public int getMaxOrders() {
        return maxOrders;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public BigDecimal getMaxVolume() {
        return maxVolume;
    }

    public BigDecimal getMaxWeight() {
        return maxWeight;
    }

    public int getMaxPickers() {
        return maxPickers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxOrders = 100;
        private int maxLines = 500;
        private BigDecimal maxVolume = new BigDecimal("1000.0");
        private BigDecimal maxWeight = new BigDecimal("5000.0");
        private int maxPickers = 10;

        public Builder maxOrders(int maxOrders) {
            this.maxOrders = maxOrders;
            return this;
        }

        public Builder maxLines(int maxLines) {
            this.maxLines = maxLines;
            return this;
        }

        public Builder maxVolume(BigDecimal maxVolume) {
            this.maxVolume = maxVolume;
            return this;
        }

        public Builder maxWeight(BigDecimal maxWeight) {
            this.maxWeight = maxWeight;
            return this;
        }

        public Builder maxPickers(int maxPickers) {
            this.maxPickers = maxPickers;
            return this;
        }

        public WaveCapacity build() {
            return new WaveCapacity(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaveCapacity that = (WaveCapacity) o;
        return maxOrders == that.maxOrders &&
                maxLines == that.maxLines &&
                maxPickers == that.maxPickers &&
                maxVolume.compareTo(that.maxVolume) == 0 &&
                maxWeight.compareTo(that.maxWeight) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxOrders, maxLines, maxVolume, maxWeight, maxPickers);
    }

    @Override
    public String toString() {
        return String.format("WaveCapacity{orders=%d, lines=%d, volume=%s, weight=%s, pickers=%d}",
                maxOrders, maxLines, maxVolume, maxWeight, maxPickers);
    }
}
