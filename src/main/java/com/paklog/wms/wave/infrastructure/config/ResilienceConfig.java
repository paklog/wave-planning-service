package com.paklog.wms.wave.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for circuit breakers
 * Priority 8: Circuit breakers for service communication
 */
@Configuration
public class ResilienceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ResilienceConfig.class);

    private int failureRateThreshold;

    private int slowCallRateThreshold;

    private Duration slowCallDurationThreshold;

    private Duration waitDurationInOpenState;

    private int slidingWindowSize;

    private int minimumNumberOfCalls;

    private int permittedCallsInHalfOpenState;

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slowCallRateThreshold(slowCallRateThreshold)
                .slowCallDurationThreshold(slowCallDurationThreshold)
                .waitDurationInOpenState(waitDurationInOpenState)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpenState)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Register circuit breaker for warehouse-operations
        CircuitBreaker circuitBreaker = registry.circuitBreaker("warehouse-operations");

        // Add event listeners
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    logger.warn("Circuit breaker state transition: {} -> {}",
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState());
                })
                .onError(event -> {
                    logger.debug("Circuit breaker error: {}", event.getThrowable().getMessage());
                })
                .onSuccess(event -> {
                    logger.debug("Circuit breaker success: duration={}ms",
                            event.getElapsedDuration().toMillis());
                });

        logger.info("Circuit breaker configured: failureRateThreshold={}%, slidingWindowSize={}, waitDuration={}s",
                failureRateThreshold, slidingWindowSize, waitDurationInOpenState.getSeconds());

        return registry;
    }
}
