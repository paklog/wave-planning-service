package com.paklog.wms.wave.infrastructure.shadow;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShadowModeMetricsTest {

    @Test
    void metricsTrackExecutionsMatchesMismatchesAndErrors() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ShadowModeMetrics metrics = new ShadowModeMetrics(registry);

        metrics.recordShadowExecution();
        metrics.recordMatch();
        metrics.recordMismatch();
        metrics.recordError();

        assertThat(registry.counter("shadow.mode.executions", "service", "wave-planning").count()).isEqualTo(1.0);
        assertThat(registry.counter("shadow.mode.matches", "service", "wave-planning").count()).isEqualTo(1.0);
        assertThat(registry.counter("shadow.mode.mismatches", "service", "wave-planning").count()).isEqualTo(1.0);
        assertThat(registry.counter("shadow.mode.errors", "service", "wave-planning").count()).isEqualTo(1.0);
    }
}

