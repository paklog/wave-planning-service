package com.paklog.wms.wave.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Client for calling legacy warehouse-operations service
 * Priority 8: Implement circuit breakers for service communication
 *
 * Uses Resilience4j circuit breaker to prevent cascading failures
 */
@Component
public class WarehouseOperationsClient {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseOperationsClient.class);
    private static final String CIRCUIT_BREAKER_NAME = "warehouse-operations";

    private final WebClient webClient;

    public WarehouseOperationsClient(
            @Value("${paklog.features.shadow-mode.legacy-endpoint:http://warehouse-operations:8080}") String legacyEndpoint
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(legacyEndpoint)
                .build();
    }

    /**
     * Plan wave using legacy system
     * Circuit breaker will open if failures exceed threshold
     *
     * @param waveId the wave identifier
     * @return wave planning result from legacy system
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "planWaveFallback")
    public Object planWave(String waveId) {
        logger.debug("Calling legacy system for wave planning: {}", waveId);

        try {
            return webClient.post()
                    .uri("/api/waves/plan")
                    .bodyValue(Map.of("waveId", waveId))
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            logger.error("Error calling legacy system for wave {}", waveId, e);
            throw new LegacySystemException("Failed to call legacy system", e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     * Returns null to indicate legacy system unavailable
     *
     * @param waveId the wave identifier
     * @param ex the exception that triggered the fallback
     * @return null (legacy system unavailable)
     */
    private Object planWaveFallback(String waveId, Exception ex) {
        logger.warn("Circuit breaker open for legacy system, wave: {}, error: {}",
                waveId, ex.getMessage());
        return null;
    }

    /**
     * Get wave details from legacy system
     *
     * @param waveId the wave identifier
     * @return wave details
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getWaveFallback")
    public Object getWave(String waveId) {
        logger.debug("Getting wave from legacy system: {}", waveId);

        return webClient.get()
                .uri("/api/waves/{waveId}", waveId)
                .retrieve()
                .bodyToMono(Object.class)
                .timeout(Duration.ofSeconds(3))
                .block();
    }

    /**
     * Fallback for getWave
     */
    private Object getWaveFallback(String waveId, Exception ex) {
        logger.warn("Circuit breaker open for getWave, waveId: {}", waveId);
        return null;
    }

    /**
     * Exception thrown when legacy system call fails
     */
    public static class LegacySystemException extends RuntimeException {
        public LegacySystemException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
