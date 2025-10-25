package com.paklog.wms.wave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wave Planning Service - WMS
 * Handles wave planning, workload forecasting, and capacity management
 *
 * Features:
 * - Transactional outbox pattern for reliable event publishing
 * - Circuit breakers for resilient communication with legacy system
 * - Shadow mode for gradual migration validation
 * - Reconciliation service for data consistency
 * - Distributed tracing with OpenTelemetry
 * - Feature flags for controlled rollout
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@EnableScheduling  // Enable scheduled tasks (reconciliation, outbox cleanup)
@EnableAsync      // Enable async execution (shadow mode)
public class WavePlanningServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WavePlanningServiceApplication.class, args);
    }
}
