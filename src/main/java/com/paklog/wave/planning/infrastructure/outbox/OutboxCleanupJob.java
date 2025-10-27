package com.paklog.wave.planning.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Background job to clean up old published events from the outbox in Wave Planning Service
 * Prevents unbounded growth of the outbox table
 * Copied from paklog-integration to eliminate compilation dependency
 */
@Component
@ConditionalOnProperty(
    name = "wave-planning.outbox.cleanup.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class OutboxCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final OutboxRepository outboxRepository;

    @Value("${wave-planning.outbox.cleanup.retention-days:7}")
    private int retentionDays;

    public OutboxCleanupJob(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /**
     * Clean up old published events
     * Runs daily at 2 AM by default
     */
    @Scheduled(cron = "${wave-planning.outbox.cleanup.cron:0 0 2 * * *}")
    @Transactional
    public void cleanupOldEvents() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

            log.info("Starting outbox cleanup for events published before {}", cutoffDate);

            int deletedCount = outboxRepository.deletePublishedEventsBefore(cutoffDate);

            log.info("Outbox cleanup completed. Deleted {} published events older than {} days",
                    deletedCount, retentionDays);

        } catch (Exception e) {
            log.error("Error during outbox cleanup", e);
        }
    }

    /**
     * Log outbox statistics
     * Runs every hour by default
     */
    @Scheduled(cron = "${wave-planning.outbox.stats.cron:0 0 * * * *}")
    public void logOutboxStats() {
        try {
            long pendingCount = outboxRepository.countByStatus(OutboxStatus.PENDING);
            long failedCount = outboxRepository.countByStatus(OutboxStatus.FAILED);
            long publishedCount = outboxRepository.countByStatus(OutboxStatus.PUBLISHED);

            log.info("Outbox statistics - Pending: {}, Failed: {}, Published: {}",
                    pendingCount, failedCount, publishedCount);

            if (failedCount > 100) {
                log.warn("High number of failed events in outbox: {}. Check Kafka connectivity and retry configuration.",
                        failedCount);
            }

        } catch (Exception e) {
            log.error("Error collecting outbox statistics", e);
        }
    }
}
