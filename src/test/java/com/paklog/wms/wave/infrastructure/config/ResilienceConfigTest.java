package com.paklog.wms.wave.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ResilienceConfigTest {

    @Test
    void circuitBreakerRegistryUsesConfiguredThresholds() {
        ResilienceConfig config = new ResilienceConfig();
        ReflectionTestUtils.setField(config, "failureRateThreshold", 25);
        ReflectionTestUtils.setField(config, "slowCallRateThreshold", 30);
        ReflectionTestUtils.setField(config, "slowCallDurationThreshold", Duration.ofSeconds(3));
        ReflectionTestUtils.setField(config, "waitDurationInOpenState", Duration.ofSeconds(15));
        ReflectionTestUtils.setField(config, "slidingWindowSize", 20);
        ReflectionTestUtils.setField(config, "minimumNumberOfCalls", 5);
        ReflectionTestUtils.setField(config, "permittedCallsInHalfOpenState", 2);

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry(meterRegistry);

        CircuitBreaker circuitBreaker = registry.circuitBreaker("warehouse-operations");
        CircuitBreakerConfig breakerConfig = circuitBreaker.getCircuitBreakerConfig();

        assertThat(breakerConfig.getFailureRateThreshold()).isEqualTo(25f);
        assertThat(breakerConfig.getSlowCallRateThreshold()).isEqualTo(30f);
        assertThat(breakerConfig.getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(3));
        long waitMillis = breakerConfig.getWaitIntervalFunctionInOpenState().apply(1);
        assertThat(waitMillis).isEqualTo(Duration.ofSeconds(15).toMillis());
        assertThat(breakerConfig.getSlidingWindowSize()).isEqualTo(20);
        assertThat(breakerConfig.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(breakerConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(2);
    }
}
