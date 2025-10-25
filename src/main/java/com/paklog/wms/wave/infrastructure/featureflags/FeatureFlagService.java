package com.paklog.wms.wave.infrastructure.featureflags;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Feature flag service for gradual rollout and A/B testing
 * Priority 9: Feature flag routing mechanism
 */
@Service
public class FeatureFlagService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureFlagService.class);
    private final Random random = new Random();

    private boolean shadowModeEnabled;

    private int shadowModePercentage;

    private boolean reconciliationEnabled;

    /**
     * Check if shadow mode is enabled for this request
     * Uses percentage-based routing for gradual rollout
     *
     * @return true if request should use shadow mode
     */
    public boolean isShadowModeEnabled() {
        if (!shadowModeEnabled) {
            return false;
        }

        // Percentage-based routing (0-100)
        if (shadowModePercentage == 0) {
            return false;
        }

        if (shadowModePercentage == 100) {
            return true;
        }

        // Random sampling based on percentage
        int sample = random.nextInt(100);
        boolean enabled = sample < shadowModePercentage;

        logger.debug("Shadow mode check: percentage={}, sample={}, enabled={}",
                shadowModePercentage, sample, enabled);

        return enabled;
    }

    /**
     * Check if shadow mode is enabled for a specific wave
     * Can be extended to support per-wave, per-warehouse, or per-user flags
     *
     * @param waveId the wave identifier
     * @return true if shadow mode enabled for this wave
     */
    public boolean isShadowModeEnabled(String waveId) {
        if (!shadowModeEnabled) {
            return false;
        }

        // For now, use same percentage-based logic
        // Future: Implement wave-specific targeting
        // e.g., enable for specific warehouses, customers, or priority levels
        return isShadowModeEnabled();
    }

    /**
     * Check if reconciliation is enabled
     *
     * @return true if reconciliation enabled
     */
    public boolean isReconciliationEnabled() {
        return reconciliationEnabled;
    }

    /**
     * Get current shadow mode percentage
     *
     * @return percentage (0-100)
     */
    public int getShadowModePercentage() {
        return shadowModePercentage;
    }

    /**
     * Check if a feature is enabled
     *
     * @param featureName the feature name
     * @return true if feature enabled
     */
    public boolean isFeatureEnabled(String featureName) {
        return switch (featureName) {
            case "shadow-mode" -> shadowModeEnabled;
            case "reconciliation" -> reconciliationEnabled;
            default -> {
                logger.warn("Unknown feature flag: {}", featureName);
                yield false;
            }
        };
    }
}
