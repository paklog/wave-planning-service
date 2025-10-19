package com.paklog.wms.wave.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaveStrategyTest {

    @Test
    void builderCreatesValidStrategy() {
        WaveStrategy strategy = WaveStrategy.builder()
                .type(WaveStrategyType.CARRIER_BASED)
                .maxOrders(50)
                .maxLines(200)
                .timeInterval(Duration.ofMinutes(45))
                .build();

        assertThat(strategy.getType()).isEqualTo(WaveStrategyType.CARRIER_BASED);
        assertThat(strategy.getMaxOrders()).isEqualTo(50);
        assertThat(strategy.getTimeInterval()).isEqualTo(Duration.ofMinutes(45));
    }

    @Test
    void builderRejectsInvalidValues() {
        assertThatThrownBy(() -> WaveStrategy.builder()
                .type(WaveStrategyType.ZONE_BASED)
                .maxOrders(0)
                .build()).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> WaveStrategy.builder()
                .type(WaveStrategyType.ZONE_BASED)
                .maxLines(-1)
                .build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void timeBasedStrategyRequiresInterval() {
        assertThatThrownBy(() -> WaveStrategy.builder()
                .type(WaveStrategyType.TIME_BASED)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Time interval required");
    }

    @Test
    void capacityCheckRespectsLimits() {
        WaveStrategy strategy = WaveStrategy.builder()
                .type(WaveStrategyType.PRIORITY_BASED)
                .maxOrders(5)
                .maxLines(10)
                .timeInterval(Duration.ofMinutes(30))
                .build();

        assertThat(strategy.hasCapacityFor(5, 5)).isTrue();
        assertThat(strategy.hasCapacityFor(6, 5)).isFalse();
        assertThat(strategy.hasCapacityFor(5, 11)).isFalse();
    }

    @Test
    void defaultStrategyProvidesReasonableDefaults() {
        WaveStrategy strategy = WaveStrategy.defaultStrategy();

        assertThat(strategy.getType()).isEqualTo(WaveStrategyType.TIME_BASED);
        assertThat(strategy.getMaxOrders()).isEqualTo(100);
        assertThat(strategy.getTimeInterval()).isEqualTo(Duration.ofHours(1));
    }
}
