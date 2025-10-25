package com.paklog.wms.wave.infrastructure.reconciliation;

import com.paklog.wms.wave.infrastructure.reconciliation.ReconciliationService.ReconciliationReport;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationMetricsTest {

    @Test
    void metricsCaptureReconciliationCountersAndGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ReconciliationMetrics metrics = new ReconciliationMetrics(registry);

        ReconciliationReport report = new ReconciliationReport(
                LocalDateTime.now(),
                10,
                List.of("WAVE-1 mismatch", "WAVE-2 mismatch"),
                List.of("WAVE-3 error"),
                20.0
        );

        metrics.recordReconciliation(report);
        metrics.recordHighVariance();
        metrics.recordError();

        assertThat(registry.counter("reconciliation.runs", "service", "wave-planning").count()).isEqualTo(1.0);
        assertThat(registry.counter("reconciliation.mismatches", "service", "wave-planning").count()).isEqualTo(2.0);
        assertThat(registry.counter("reconciliation.errors", "service", "wave-planning").count()).isEqualTo(2.0);
        assertThat(registry.counter("reconciliation.high_variance", "service", "wave-planning").count()).isEqualTo(1.0);

        Gauge mismatchGauge = registry.find("reconciliation.last_mismatch_count").tag("service", "wave-planning").gauge();
        Gauge checkedGauge = registry.find("reconciliation.last_checked_count").tag("service", "wave-planning").gauge();

        assertThat(mismatchGauge.value()).isEqualTo(2.0);
        assertThat(checkedGauge.value()).isEqualTo(10.0);
    }
}

