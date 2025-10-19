package com.paklog.wms.wave.infrastructure.reconciliation;

import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.repository.WaveRepository;
import com.paklog.wms.wave.infrastructure.featureflags.FeatureFlagService;
import com.paklog.wms.wave.infrastructure.resilience.WarehouseOperationsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reconciliation service to detect and fix data inconsistencies
 * Priority 6: Build reconciliation service for data validation
 *
 * Runs periodically to compare data between new and legacy systems
 * during migration period
 */
@Service
@ConditionalOnProperty(name = "paklog.features.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
public class ReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    private final WaveRepository waveRepository;
    private final WarehouseOperationsClient legacyClient;
    private final FeatureFlagService featureFlagService;
    private final ReconciliationMetrics metrics;

    @Value("${paklog.features.reconciliation.max-variance-percentage:5.0}")
    private double maxVariancePercentage;

    public ReconciliationService(
            WaveRepository waveRepository,
            WarehouseOperationsClient legacyClient,
            FeatureFlagService featureFlagService,
            ReconciliationMetrics metrics
    ) {
        this.waveRepository = waveRepository;
        this.legacyClient = legacyClient;
        this.featureFlagService = featureFlagService;
        this.metrics = metrics;
    }

    /**
     * Run reconciliation job
     * Scheduled via cron expression in configuration
     */
    @Scheduled(cron = "${paklog.features.reconciliation.schedule:0 0 */6 * * *}")
    public void runReconciliation() {
        if (!featureFlagService.isReconciliationEnabled()) {
            logger.debug("Reconciliation disabled via feature flag");
            return;
        }

        logger.info("Starting reconciliation job");
        long startTime = System.currentTimeMillis();

        try {
            ReconciliationReport report = reconcileWaves();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Reconciliation completed in {}ms: checked={}, mismatches={}, errors={}",
                    duration, report.totalChecked(), report.mismatches().size(), report.errors().size());

            metrics.recordReconciliation(report);

            // Alert if mismatches exceed threshold
            if (report.mismatchPercentage() > maxVariancePercentage) {
                logger.error("Reconciliation variance {} exceeds threshold {}%",
                        report.mismatchPercentage(), maxVariancePercentage);
                metrics.recordHighVariance();
            }

        } catch (Exception e) {
            logger.error("Reconciliation job failed", e);
            metrics.recordError();
        }
    }

    /**
     * Reconcile waves between new and legacy systems
     *
     * @return reconciliation report
     */
    private ReconciliationReport reconcileWaves() {
        List<String> mismatches = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalChecked = 0;

        // Get recent waves (last 24 hours)
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        List<Wave> recentWaves = waveRepository.findWavesCreatedAfter(since);

        for (Wave wave : recentWaves) {
            totalChecked++;

            try {
                // Get wave from legacy system
                Object legacyWave = legacyClient.getWave(wave.getWaveId());

                if (legacyWave == null) {
                    mismatches.add(String.format("Wave %s not found in legacy system", wave.getWaveId()));
                    continue;
                }

                // Compare waves
                if (!compareWaves(wave, legacyWave)) {
                    mismatches.add(String.format("Wave %s data mismatch", wave.getWaveId()));
                }

            } catch (Exception e) {
                logger.error("Error reconciling wave {}", wave.getWaveId(), e);
                errors.add(String.format("Wave %s error: %s", wave.getWaveId(), e.getMessage()));
            }
        }

        double mismatchPercentage = totalChecked > 0
                ? (mismatches.size() * 100.0) / totalChecked
                : 0.0;

        return new ReconciliationReport(
                LocalDateTime.now(),
                totalChecked,
                mismatches,
                errors,
                mismatchPercentage
        );
    }

    /**
     * Compare wave data between systems
     * Can be customized based on comparison strategy
     *
     * @param newWave wave from new system
     * @param legacyWave wave from legacy system
     * @return true if waves match
     */
    private boolean compareWaves(Wave newWave, Object legacyWave) {
        // Implement comparison logic
        // Compare:
        // - Wave status
        // - Order list
        // - Priority
        // - Zone assignment
        // - Strategy
        // etc.

        // For now, simple placeholder
        return true;
    }

    /**
     * Reconciliation report
     */
    public record ReconciliationReport(
            LocalDateTime timestamp,
            int totalChecked,
            List<String> mismatches,
            List<String> errors,
            double mismatchPercentage
    ) {
    }
}
