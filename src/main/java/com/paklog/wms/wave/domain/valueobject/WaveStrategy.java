package com.paklog.wms.wave.domain.valueobject;

import java.time.Duration;
import java.util.Objects;

/**
 * Value object representing wave planning strategy configuration
 * Immutable configuration for how waves should be planned
 */
public class WaveStrategy {

    private final WaveStrategyType type;
    private final Integer maxWaveSize;
    private final Integer maxOrders;
    private final Integer maxLines;
    private final Duration timeInterval;

    private WaveStrategy(Builder builder) {
        this.type = builder.type;
        this.maxWaveSize = builder.maxWaveSize;
        this.maxOrders = builder.maxOrders;
        this.maxLines = builder.maxLines;
        this.timeInterval = builder.timeInterval;
        validate();
    }

    private void validate() {
        if (type == null) {
            throw new IllegalArgumentException("Strategy type cannot be null");
        }
        if (maxWaveSize != null && maxWaveSize <= 0) {
            throw new IllegalArgumentException("Max wave size must be positive");
        }
        if (maxOrders != null && maxOrders <= 0) {
            throw new IllegalArgumentException("Max orders must be positive");
        }
        if (maxLines != null && maxLines <= 0) {
            throw new IllegalArgumentException("Max lines must be positive");
        }
        if (type == WaveStrategyType.TIME_BASED && timeInterval == null) {
            throw new IllegalArgumentException("Time interval required for TIME_BASED strategy");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static WaveStrategy defaultStrategy() {
        return builder()
                .type(WaveStrategyType.TIME_BASED)
                .maxOrders(100)
                .maxLines(500)
                .timeInterval(Duration.ofHours(1))
                .build();
    }

    public WaveStrategyType getType() {
        return type;
    }

    public Integer getMaxWaveSize() {
        return maxWaveSize;
    }

    public Integer getMaxOrders() {
        return maxOrders;
    }

    public Integer getMaxLines() {
        return maxLines;
    }

    public Duration getTimeInterval() {
        return timeInterval;
    }

    public boolean hasCapacityFor(int orderCount, int lineCount) {
        if (maxOrders != null && orderCount > maxOrders) {
            return false;
        }
        if (maxLines != null && lineCount > maxLines) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaveStrategy that = (WaveStrategy) o;
        return type == that.type &&
                Objects.equals(maxWaveSize, that.maxWaveSize) &&
                Objects.equals(maxOrders, that.maxOrders) &&
                Objects.equals(maxLines, that.maxLines) &&
                Objects.equals(timeInterval, that.timeInterval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, maxWaveSize, maxOrders, maxLines, timeInterval);
    }

    @Override
    public String toString() {
        return "WaveStrategy{" +
                "type=" + type +
                ", maxWaveSize=" + maxWaveSize +
                ", maxOrders=" + maxOrders +
                ", maxLines=" + maxLines +
                ", timeInterval=" + timeInterval +
                '}';
    }

    public static class Builder {
        private WaveStrategyType type;
        private Integer maxWaveSize;
        private Integer maxOrders;
        private Integer maxLines;
        private Duration timeInterval;

        public Builder type(WaveStrategyType type) {
            this.type = type;
            return this;
        }

        public Builder maxWaveSize(Integer maxWaveSize) {
            this.maxWaveSize = maxWaveSize;
            return this;
        }

        public Builder maxOrders(Integer maxOrders) {
            this.maxOrders = maxOrders;
            return this;
        }

        public Builder maxLines(Integer maxLines) {
            this.maxLines = maxLines;
            return this;
        }

        public Builder timeInterval(Duration timeInterval) {
            this.timeInterval = timeInterval;
            return this;
        }

        public WaveStrategy build() {
            return new WaveStrategy(this);
        }
    }
}
