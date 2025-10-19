package com.paklog.wms.wave.infrastructure.shadow;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Metrics for shadow mode execution
 */
@Component
public class ShadowModeMetrics {

    private final Counter executionCounter;
    private final Counter matchCounter;
    private final Counter mismatchCounter;
    private final Counter errorCounter;

    public ShadowModeMetrics(MeterRegistry meterRegistry) {
        this.executionCounter = Counter.builder("shadow.mode.executions")
                .description("Total shadow mode executions")
                .tag("service", "wave-planning")
                .register(meterRegistry);

        this.matchCounter = Counter.builder("shadow.mode.matches")
                .description("Shadow mode results that match")
                .tag("service", "wave-planning")
                .register(meterRegistry);

        this.mismatchCounter = Counter.builder("shadow.mode.mismatches")
                .description("Shadow mode results that don't match")
                .tag("service", "wave-planning")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("shadow.mode.errors")
                .description("Shadow mode execution errors")
                .tag("service", "wave-planning")
                .register(meterRegistry);
    }

    public void recordShadowExecution() {
        executionCounter.increment();
    }

    public void recordMatch() {
        matchCounter.increment();
    }

    public void recordMismatch() {
        mismatchCounter.increment();
    }

    public void recordError() {
        errorCounter.increment();
    }
}
