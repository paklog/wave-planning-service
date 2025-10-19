package com.paklog.wms.wave.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WaveMetricsTest {

    @Test
    void pickTimingCalculationsWork() {
        WaveMetrics metrics = new WaveMetrics();
        metrics.setPlannedPickTime(60.0);
        metrics.setStartTime(LocalDateTime.now().minusMinutes(30));

        metrics.recordPickCompletion();
        metrics.calculateEfficiency();

        assertThat(metrics.getActualPickTime()).isNotNull();
        assertThat(metrics.getLaborEfficiency()).isGreaterThan(0.0);
    }

    @Test
    void orderCompletionUpdatesRates() {
        WaveMetrics metrics = new WaveMetrics();
        metrics.setTotalOrders(10);

        metrics.updateOrderCompletion(7);

        assertThat(metrics.getCompletedOrders()).isEqualTo(7);
        assertThat(metrics.getOrderFillRate()).isEqualTo(70.0);
        assertThat(metrics.isComplete()).isFalse();

        metrics.updateOrderCompletion(10);
        assertThat(metrics.isComplete()).isTrue();
    }

    @Test
    void lineCompletionAndUnitsAreTracked() {
        WaveMetrics metrics = new WaveMetrics();
        metrics.setTotalLines(20);
        metrics.setTotalUnits(100);

        metrics.updateLineCompletion(15);

        assertThat(metrics.getCompletedLines()).isEqualTo(15);
        assertThat(metrics.getTotalUnits()).isEqualTo(100);
    }
}
