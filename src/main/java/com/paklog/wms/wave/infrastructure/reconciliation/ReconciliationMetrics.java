package com.paklog.wms.wave.infrastructure.reconciliation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Metrics for reconciliation service
 */
@Component
public class ReconciliationMetrics {

    private final Counter reconciliationCounter;
    private final Counter mismatchCounter;
    private final Counter errorCounter;
    private final Counter highVarianceCounter;
    private final AtomicInteger lastMismatchCount = new AtomicInteger(0);
    private final AtomicInteger lastCheckedCount = new AtomicInteger(0);

    public ReconciliationMetrics(MeterRegistry meterRegistry) {
        this.reconciliationCounter = Counter.builder("reconciliation.runs")
                .description("Total reconciliation runs")
                .tag("service", "wave-planning")
                .register(meterRegistry);

        this.mismatchCounter = Counter.builder("reconciliation.mismatches")
                .description("Total mismatches found")
                .tag("service", "wave-planning")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("reconciliation.errors")
                .description("Reconciliation errors")
                .tag("service", "wave-planning")
                .register(meterRegistry);

        this.highVarianceCounter = Counter.builder("reconciliation.high_variance")
                .description("High variance alerts")
                .tag("service", "wave-planning")
                .register(meterRegistry);

        // Gauges for last run
        Gauge.builder("reconciliation.last_mismatch_count", lastMismatchCount, AtomicInteger::get)
                .description("Mismatches in last reconciliation run")
                .tag("service", "wave-planning")
                .register(meterRegistry);

        Gauge.builder("reconciliation.last_checked_count", lastCheckedCount, AtomicInteger::get)
                .description("Items checked in last reconciliation run")
                .tag("service", "wave-planning")
                .register(meterRegistry);
    }

    public void recordReconciliation(ReconciliationService.ReconciliationReport report) {
        reconciliationCounter.increment();
        mismatchCounter.increment(report.mismatches().size());
        errorCounter.increment(report.errors().size());

        lastMismatchCount.set(report.mismatches().size());
        lastCheckedCount.set(report.totalChecked());
    }

    public void recordError() {
        errorCounter.increment();
    }

    public void recordHighVariance() {
        highVarianceCounter.increment();
    }
}
