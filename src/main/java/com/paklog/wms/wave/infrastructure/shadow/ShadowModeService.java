package com.paklog.wms.wave.infrastructure.shadow;

import com.paklog.wms.wave.infrastructure.featureflags.FeatureFlagService;
import com.paklog.wms.wave.infrastructure.resilience.WarehouseOperationsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Shadow mode service for parallel execution and comparison
 * Priority 5: Implement shadow mode for wave planning service
 *
 * Shadow mode allows running both new and legacy systems in parallel
 * to validate correctness before full cutover
 */
@Service
@ConditionalOnProperty(name = "paklog.features.shadow-mode.enabled", havingValue = "true")
public class ShadowModeService {

    private static final Logger logger = LoggerFactory.getLogger(ShadowModeService.class);

    private final FeatureFlagService featureFlagService;
    private final WarehouseOperationsClient legacyClient;
    private final ShadowModeMetrics metrics;

    public ShadowModeService(
            FeatureFlagService featureFlagService,
            WarehouseOperationsClient legacyClient,
            ShadowModeMetrics metrics
    ) {
        this.featureFlagService = featureFlagService;
        this.legacyClient = legacyClient;
        this.metrics = metrics;
    }

    /**
     * Execute wave planning in shadow mode
     * Calls legacy system asynchronously and compares results
     *
     * @param waveId the wave identifier
     * @param newResult result from new system
     */
    @Async
    public CompletableFuture<ShadowModeResult> executeShadowCall(String waveId, Object newResult) {
        if (!featureFlagService.isShadowModeEnabled(waveId)) {
            return CompletableFuture.completedFuture(
                    new ShadowModeResult(waveId, true, "Shadow mode disabled", null)
            );
        }

        logger.info("Executing shadow mode for wave: {}", waveId);
        metrics.recordShadowExecution();

        try {
            // Call legacy system
            Object legacyResult = legacyClient.planWave(waveId);

            // Compare results
            boolean matches = compareResults(newResult, legacyResult);

            if (!matches) {
                logger.warn("Shadow mode mismatch for wave {}: new={}, legacy={}",
                        waveId, newResult, legacyResult);
                metrics.recordMismatch();
            } else {
                logger.debug("Shadow mode match for wave {}", waveId);
                metrics.recordMatch();
            }

            return CompletableFuture.completedFuture(
                    new ShadowModeResult(waveId, matches,
                            matches ? "Results match" : "Results mismatch",
                            Map.of("new", newResult, "legacy", legacyResult))
            );

        } catch (Exception e) {
            logger.error("Shadow mode execution failed for wave {}", waveId, e);
            metrics.recordError();
            return CompletableFuture.completedFuture(
                    new ShadowModeResult(waveId, false, "Error: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Compare results from new and legacy systems
     * Can be customized based on comparison strategy
     *
     * @param newResult result from new system
     * @param legacyResult result from legacy system
     * @return true if results match
     */
    private boolean compareResults(Object newResult, Object legacyResult) {
        if (newResult == null && legacyResult == null) {
            return true;
        }

        if (newResult == null || legacyResult == null) {
            return false;
        }

        // Simple equality check
        // Can be extended to compare specific fields, ignore timestamps, etc.
        return newResult.equals(legacyResult);
    }

    /**
     * Result of shadow mode execution
     */
    public record ShadowModeResult(
            String waveId,
            boolean matches,
            String message,
            Map<String, Object> details
    ) {
    }
}
