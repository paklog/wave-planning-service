# Wave Planning Service - Decoupling Plan
## Eliminate Dependencies on Shared Modules

**Service:** wave-planning-service
**Complexity:** HIGH (uses Outbox pattern)
**Estimated Effort:** 8 hours
**Priority:** Phase 1 (complete first - publishes events consumed by other services)

---

## Current Dependencies

### Shared Modules Used
- ✓ **paklog-domain** (v0.0.1-SNAPSHOT)
  - `com.paklog.domain.annotation.AggregateRoot`
  - `com.paklog.domain.shared.DomainEvent`

- ✓ **paklog-integration** (v0.0.1-SNAPSHOT)
  - `com.paklog.integration.outbox.OutboxEvent`
  - `com.paklog.integration.outbox.OutboxRepository`
  - `com.paklog.integration.outbox.OutboxService`
  - `com.paklog.integration.outbox.OutboxStatus`
  - `com.paklog.integration.outbox.OutboxCleanupJob`
  - `com.paklog.integration.outbox.OutboxRelay`
  - `com.paklog.integration.config.OutboxConfiguration`

### Coupling Impact
- Cannot deploy independently
- Breaking changes in shared modules affect this service
- Build requires shared module artifacts
- Testing requires shared module dependencies

---

## Target Architecture

### Service-Owned Components
```
wave-planning-service/
├── src/main/java/com/paklog/wave/planning/
│   ├── domain/
│   │   └── shared/
│   │       ├── AggregateRoot.java        # Copy from paklog-domain
│   │       └── DomainEvent.java          # Copy from paklog-domain
│   │
│   ├── events/                            # Publisher-owned schemas
│   │   ├── WaveCreatedEvent.java
│   │   ├── WaveReleasedEvent.java
│   │   ├── WaveCompletedEvent.java
│   │   └── WaveCancelledEvent.java
│   │
│   └── infrastructure/
│       ├── outbox/                        # Copy from paklog-integration
│       │   ├── OutboxEvent.java
│       │   ├── OutboxRepository.java
│       │   ├── OutboxService.java
│       │   ├── OutboxStatus.java
│       │   ├── OutboxCleanupJob.java
│       │   ├── OutboxRelay.java
│       │   └── OutboxException.java
│       │
│       ├── config/
│       │   └── OutboxConfiguration.java
│       │
│       └── events/
│           └── WaveEventPublisher.java    # CloudEvents publisher
```

---

## CloudEvents Schema Definition

### Event Type Pattern
All events MUST follow: `com.paklog.wms.wave-planning.wave.<entity>.<action>`

### Events Published by Wave Planning Service

#### 1. Wave Created Event
**Type:** `com.paklog.wms.wave-planning.wave.wave.created.v1`
**Trigger:** New wave is created
**Schema:**
```json
{
  "wave_id": "WAVE-12345",
  "wave_number": "W-2025-001",
  "wave_type": "BATCH|DISCRETE|ZONE",
  "priority": "URGENT|HIGH|NORMAL|LOW",
  "order_count": 150,
  "total_lines": 450,
  "zone_id": "ZONE-A1",
  "created_at": "2025-10-26T10:00:00Z",
  "target_completion_time": "2025-10-26T14:00:00Z"
}
```

#### 2. Wave Released Event
**Type:** `com.paklog.wms.wave-planning.wave.wave.released.v1`
**Trigger:** Wave is released for picking
**Schema:**
```json
{
  "wave_id": "WAVE-12345",
  "wave_number": "W-2025-001",
  "priority": "HIGH",
  "order_ids": ["ORD-001", "ORD-002", "ORD-003"],
  "total_lines": 450,
  "zone_id": "ZONE-A1",
  "released_at": "2025-10-26T10:05:00Z",
  "target_completion_time": "2025-10-26T14:00:00Z",
  "carrier_cutoff_time": "2025-10-26T15:00:00Z"
}
```

#### 3. Wave Completed Event
**Type:** `com.paklog.wms.wave-planning.wave.wave.completed.v1`
**Trigger:** All tasks in wave are completed
**Schema:**
```json
{
  "wave_id": "WAVE-12345",
  "wave_number": "W-2025-001",
  "completed_at": "2025-10-26T13:45:00Z",
  "duration_minutes": 220,
  "orders_completed": 150,
  "lines_picked": 450,
  "success_rate": 0.98
}
```

#### 4. Wave Cancelled Event
**Type:** `com.paklog.wms.wave-planning.wave.wave.cancelled.v1`
**Trigger:** Wave is cancelled before completion
**Schema:**
```json
{
  "wave_id": "WAVE-12345",
  "wave_number": "W-2025-001",
  "cancelled_at": "2025-10-26T11:30:00Z",
  "reason": "Insufficient inventory|Carrier delay|Manual cancellation",
  "orders_affected": ["ORD-001", "ORD-002"]
}
```

---

## Step-by-Step Migration Tasks

### Phase 1: Preparation (30 minutes)

#### Task 1.1: Create Feature Branch
```bash
cd wave-planning-service
git checkout -b decouple/remove-shared-dependencies
```

#### Task 1.2: Run Baseline Tests
```bash
mvn clean test
# Document current test results and coverage
```

#### Task 1.3: Create Service-Internal Packages
```bash
mkdir -p src/main/java/com/paklog/wave/planning/domain/shared
mkdir -p src/main/java/com/paklog/wave/planning/events
mkdir -p src/main/java/com/paklog/wave/planning/infrastructure/outbox
mkdir -p src/main/java/com/paklog/wave/planning/infrastructure/config
mkdir -p src/main/java/com/paklog/wave/planning/infrastructure/events
```

---

### Phase 2: Internalize Domain Primitives (1 hour)

#### Task 2.1: Copy AggregateRoot Annotation
```bash
# Copy file
cp ../../paklog-domain/src/main/java/com/paklog/domain/annotation/AggregateRoot.java \
   src/main/java/com/paklog/wave/planning/domain/shared/AggregateRoot.java
```

**Update package declaration:**
```java
// File: src/main/java/com/paklog/wave/planning/domain/shared/AggregateRoot.java
package com.paklog.wave.planning.domain.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for aggregate roots in Wave Planning bounded context
 * Copied from paklog-domain to eliminate compilation dependency
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AggregateRoot {
}
```

#### Task 2.2: Copy DomainEvent Interface
```bash
# Copy file
cp ../../paklog-domain/src/main/java/com/paklog/domain/shared/DomainEvent.java \
   src/main/java/com/paklog/wave/planning/domain/shared/DomainEvent.java
```

**Update package declaration:**
```java
// File: src/main/java/com/paklog/wave/planning/domain/shared/DomainEvent.java
package com.paklog.wave.planning.domain.shared;

import java.time.Instant;

/**
 * Base interface for all domain events in Wave Planning bounded context
 * Copied from paklog-domain to eliminate compilation dependency
 */
public interface DomainEvent {

    /**
     * When the event occurred
     */
    Instant occurredOn();

    /**
     * Type of the event
     */
    String eventType();
}
```

#### Task 2.3: Update All Imports
```bash
# Find and replace imports across the codebase
find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.annotation\.AggregateRoot/import com.paklog.wave.planning.domain.shared.AggregateRoot/g' {} +

find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.shared\.DomainEvent/import com.paklog.wave.planning.domain.shared.DomainEvent/g' {} +

# Also update test files
find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.annotation\.AggregateRoot/import com.paklog.wave.planning.domain.shared.AggregateRoot/g' {} +

find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.shared\.DomainEvent/import com.paklog.wave.planning.domain.shared.DomainEvent/g' {} +
```

#### Task 2.4: Remove paklog-domain Dependency
**Edit pom.xml:**
```xml
<!-- DELETE THIS DEPENDENCY -->
<!--
<dependency>
    <groupId>com.paklog.common</groupId>
    <artifactId>paklog-domain</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
-->
```

#### Task 2.5: Verify Compilation
```bash
mvn clean compile
# Should succeed without paklog-domain dependency
```

---

### Phase 3: Internalize Outbox Pattern (2 hours)

#### Task 3.1: Copy Outbox Pattern Files

```bash
# Copy all outbox classes
cp ../../paklog-integration/src/main/java/com/paklog/integration/outbox/*.java \
   src/main/java/com/paklog/wave/planning/infrastructure/outbox/

# Copy configuration
cp ../../paklog-integration/src/main/java/com/paklog/integration/config/OutboxConfiguration.java \
   src/main/java/com/paklog/wave/planning/infrastructure/config/
```

#### Task 3.2: Update OutboxEvent Entity

**File:** `src/main/java/com/paklog/wave/planning/infrastructure/outbox/OutboxEvent.java`

```java
package com.paklog.wave.planning.infrastructure.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbox pattern entity for Wave Planning Service
 * Ensures transactional event publishing with at-least-once delivery
 */
@Entity
@Table(name = "wave_planning_outbox_events", indexes = {
    @Index(name = "idx_status_created", columnList = "status,created_at"),
    @Index(name = "idx_aggregate_id", columnList = "aggregate_id")
})
public class OutboxEvent {

    @Id
    private String id;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Version
    private Long version;

    protected OutboxEvent() {
        // JPA requires default constructor
    }

    private OutboxEvent(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.aggregateId = builder.aggregateId;
        this.eventType = builder.eventType;
        this.payload = builder.payload;
        this.status = builder.status;
        this.createdAt = Instant.now();
        this.retryCount = 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getRetryCount() { return retryCount; }
    public Long getVersion() { return version; }

    // Business methods
    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public static class Builder {
        private String aggregateId;
        private String eventType;
        private String payload;
        private OutboxStatus status = OutboxStatus.PENDING;

        public Builder aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder status(OutboxStatus status) {
            this.status = status;
            return this;
        }

        public OutboxEvent build() {
            return new OutboxEvent(this);
        }
    }
}
```

#### Task 3.3: Update All Outbox Package Declarations

```bash
# Update package in all copied files
find src/main/java/com/paklog/wave/planning/infrastructure/outbox -name "*.java" -type f -exec sed -i '' \
  's/package com\.paklog\.integration\.outbox/package com.paklog.wave.planning.infrastructure.outbox/g' {} +

# Update configuration
sed -i '' 's/package com\.paklog\.integration\.config/package com.paklog.wave.planning.infrastructure.config/g' \
  src/main/java/com/paklog/wave/planning/infrastructure/config/OutboxConfiguration.java
```

#### Task 3.4: Update Outbox Imports

```bash
# Update imports in service code
find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.integration\.outbox/import com.paklog.wave.planning.infrastructure.outbox/g' {} +

find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.integration\.config\.OutboxConfiguration/import com.paklog.wave.planning.infrastructure.config.OutboxConfiguration/g' {} +

# Update test files
find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.integration\.outbox/import com.paklog.wave.planning.infrastructure.outbox/g' {} +
```

#### Task 3.5: Update Application Configuration

**File:** `src/main/resources/application.yml`

```yaml
# Wave Planning Service - Outbox Configuration
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
    hibernate:
      ddl-auto: validate

# Outbox Relay Configuration
outbox:
  relay:
    enabled: true
    poll-interval-ms: 1000      # Poll every 1 second
    batch-size: 100             # Process up to 100 events per batch
    max-retry-attempts: 3       # Retry failed events up to 3 times
    cleanup-enabled: true
    cleanup-interval-hours: 24  # Clean up old events every 24 hours
    retention-days: 7           # Keep published events for 7 days
```

#### Task 3.6: Create Database Migration Script

**File:** `src/main/resources/db/migration/V1_1__create_outbox_table.sql`

```sql
-- Wave Planning Outbox Events Table
CREATE TABLE IF NOT EXISTS wave_planning_outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    retry_count INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,

    INDEX idx_status_created (status, created_at),
    INDEX idx_aggregate_id (aggregate_id)
);

-- For PostgreSQL, use this instead:
-- CREATE TABLE IF NOT EXISTS wave_planning_outbox_events (
--     id VARCHAR(36) PRIMARY KEY,
--     aggregate_id VARCHAR(100) NOT NULL,
--     event_type VARCHAR(200) NOT NULL,
--     payload TEXT NOT NULL,
--     status VARCHAR(20) NOT NULL,
--     created_at TIMESTAMP NOT NULL,
--     published_at TIMESTAMP,
--     retry_count INTEGER NOT NULL DEFAULT 0,
--     version BIGINT NOT NULL DEFAULT 0
-- );
--
-- CREATE INDEX idx_status_created ON wave_planning_outbox_events(status, created_at);
-- CREATE INDEX idx_aggregate_id ON wave_planning_outbox_events(aggregate_id);
```

#### Task 3.7: Remove paklog-integration Dependency

**Edit pom.xml:**
```xml
<!-- DELETE THIS DEPENDENCY -->
<!--
<dependency>
    <groupId>com.paklog.common</groupId>
    <artifactId>paklog-integration</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
-->
```

#### Task 3.8: Verify Compilation
```bash
mvn clean compile
# Should succeed without paklog-integration dependency
```

---

### Phase 4: Define Event Schemas (2 hours)

#### Task 4.1: Create WaveCreatedEvent

**File:** `src/main/java/com/paklog/wave/planning/events/WaveCreatedEvent.java`

```java
package com.paklog.wave.planning.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when a new wave is created
 * CloudEvent Type: com.paklog.wms.wave-planning.wave.wave.created.v1
 */
public record WaveCreatedEvent(
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("wave_number") String waveNumber,
    @JsonProperty("wave_type") String waveType,
    @JsonProperty("priority") String priority,
    @JsonProperty("order_count") int orderCount,
    @JsonProperty("total_lines") int totalLines,
    @JsonProperty("zone_id") String zoneId,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("target_completion_time") Instant targetCompletionTime
) {
    public static final String EVENT_TYPE = "com.paklog.wms.wave-planning.wave.wave.created.v1";
}
```

#### Task 4.2: Create WaveReleasedEvent

**File:** `src/main/java/com/paklog/wave/planning/events/WaveReleasedEvent.java`

```java
package com.paklog.wave.planning.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Event published when wave is released for picking
 * CloudEvent Type: com.paklog.wms.wave-planning.wave.wave.released.v1
 */
public record WaveReleasedEvent(
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("wave_number") String waveNumber,
    @JsonProperty("priority") String priority,
    @JsonProperty("order_ids") List<String> orderIds,
    @JsonProperty("total_lines") int totalLines,
    @JsonProperty("zone_id") String zoneId,
    @JsonProperty("released_at") Instant releasedAt,
    @JsonProperty("target_completion_time") Instant targetCompletionTime,
    @JsonProperty("carrier_cutoff_time") Instant carrierCutoffTime
) {
    public static final String EVENT_TYPE = "com.paklog.wms.wave-planning.wave.wave.released.v1";
}
```

#### Task 4.3: Create WaveCompletedEvent

**File:** `src/main/java/com/paklog/wave/planning/events/WaveCompletedEvent.java`

```java
package com.paklog.wave.planning.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when all tasks in wave are completed
 * CloudEvent Type: com.paklog.wms.wave-planning.wave.wave.completed.v1
 */
public record WaveCompletedEvent(
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("wave_number") String waveNumber,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("duration_minutes") long durationMinutes,
    @JsonProperty("orders_completed") int ordersCompleted,
    @JsonProperty("lines_picked") int linesPicked,
    @JsonProperty("success_rate") double successRate
) {
    public static final String EVENT_TYPE = "com.paklog.wms.wave-planning.wave.wave.completed.v1";
}
```

#### Task 4.4: Create WaveCancelledEvent

**File:** `src/main/java/com/paklog/wave/planning/events/WaveCancelledEvent.java`

```java
package com.paklog.wave.planning.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Event published when wave is cancelled before completion
 * CloudEvent Type: com.paklog.wms.wave-planning.wave.wave.cancelled.v1
 */
public record WaveCancelledEvent(
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("wave_number") String waveNumber,
    @JsonProperty("cancelled_at") Instant cancelledAt,
    @JsonProperty("reason") String reason,
    @JsonProperty("orders_affected") List<String> ordersAffected
) {
    public static final String EVENT_TYPE = "com.paklog.wms.wave-planning.wave.wave.cancelled.v1";
}
```

#### Task 4.5: Create CloudEvents Publisher

**File:** `src/main/java/com/paklog/wave/planning/infrastructure/events/WaveEventPublisher.java`

```java
package com.paklog.wave.planning.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wave.planning.infrastructure.outbox.OutboxService;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Publisher for Wave Planning events using CloudEvents format
 * Events are written to outbox for transactional publishing
 */
@Service
public class WaveEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WaveEventPublisher.class);
    private static final String SOURCE = "paklog://wave-planning-service";

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public WaveEventPublisher(OutboxService outboxService, ObjectMapper objectMapper) {
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish event to outbox (transactional)
     * @param aggregateId the wave identifier
     * @param eventType the CloudEvents type
     * @param eventData the event payload
     */
    @Transactional
    public void publish(String aggregateId, String eventType, Object eventData) {
        try {
            outboxService.saveEvent(aggregateId, eventType, eventData);
            log.info("Event saved to outbox: type={}, aggregateId={}", eventType, aggregateId);
        } catch (Exception e) {
            log.error("Failed to save event to outbox: type={}, aggregateId={}", eventType, aggregateId, e);
            throw e;
        }
    }

    /**
     * Build CloudEvent from event data
     * This is called by OutboxRelay when publishing to Kafka
     */
    public CloudEvent buildCloudEvent(String eventType, String eventData) {
        return CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withSource(URI.create(SOURCE))
            .withType(eventType)
            .withDataContentType("application/json")
            .withTime(OffsetDateTime.now())
            .withData(eventData.getBytes())
            .build();
    }
}
```

---

### Phase 5: Testing & Validation (2 hours)

#### Task 5.1: Update Unit Tests
```bash
# Run all unit tests
mvn test

# Fix any failing tests due to package changes
# Update imports in test files as needed
```

#### Task 5.2: Create Outbox Integration Test

**File:** `src/test/java/com/paklog/wave/planning/infrastructure/outbox/OutboxIntegrationTest.java`

```java
package com.paklog.wave.planning.infrastructure.outbox;

import com.paklog.wave.planning.events.WaveReleasedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OutboxIntegrationTest {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    @Transactional
    void shouldSaveEventToOutbox() {
        // Given
        WaveReleasedEvent event = new WaveReleasedEvent(
            "WAVE-001",
            "W-001",
            "HIGH",
            List.of("ORD-1", "ORD-2"),
            100,
            "ZONE-A1",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Instant.now().plusSeconds(7200)
        );

        // When
        OutboxEvent outboxEvent = outboxService.saveEvent(
            event.waveId(),
            WaveReleasedEvent.EVENT_TYPE,
            event
        );

        // Then
        assertThat(outboxEvent.getId()).isNotNull();
        assertThat(outboxEvent.getAggregateId()).isEqualTo("WAVE-001");
        assertThat(outboxEvent.getEventType()).isEqualTo(WaveReleasedEvent.EVENT_TYPE);
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // Verify persisted
        OutboxEvent persisted = outboxRepository.findById(outboxEvent.getId()).orElseThrow();
        assertThat(persisted.getAggregateId()).isEqualTo("WAVE-001");
    }

    @Test
    void shouldGetPendingEventCount() {
        // When
        long count = outboxService.getPendingEventCount();

        // Then
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
}
```

#### Task 5.3: Create Event Publishing Test

**File:** `src/test/java/com/paklog/wave/planning/infrastructure/events/WaveEventPublisherTest.java`

```java
package com.paklog.wave.planning.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wave.planning.events.WaveReleasedEvent;
import com.paklog.wave.planning.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WaveEventPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WaveEventPublisher publisher;

    @Test
    void shouldPublishWaveReleasedEvent() {
        // Given
        WaveReleasedEvent event = new WaveReleasedEvent(
            "WAVE-001",
            "W-001",
            "HIGH",
            List.of("ORD-1"),
            50,
            "ZONE-A1",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Instant.now().plusSeconds(7200)
        );

        // When
        publisher.publish(event.waveId(), WaveReleasedEvent.EVENT_TYPE, event);

        // Then
        verify(outboxService).saveEvent(
            eq("WAVE-001"),
            eq(WaveReleasedEvent.EVENT_TYPE),
            any(WaveReleasedEvent.class)
        );
    }
}
```

#### Task 5.4: Run Full Test Suite
```bash
mvn clean verify

# Should show:
# - All unit tests passing
# - All integration tests passing
# - Code coverage maintained
```

#### Task 5.5: Build Service Independently
```bash
mvn clean package -DskipTests

# Should succeed and produce:
# target/wave-planning-service-0.0.1-SNAPSHOT.jar
```

#### Task 5.6: Run Service Locally
```bash
mvn spring-boot:run

# Verify:
# - Application starts successfully
# - Outbox relay starts polling
# - No errors in logs
# - Health endpoint returns UP: http://localhost:8080/actuator/health
```

---

## Validation Checklist

- [ ] No compilation errors after removing shared dependencies
- [ ] All unit tests passing (mvn test)
- [ ] All integration tests passing (mvn verify)
- [ ] Code coverage maintained (≥80%)
- [ ] Service builds independently (mvn package)
- [ ] Service runs without errors (mvn spring-boot:run)
- [ ] Outbox table created in database
- [ ] OutboxRelay polls and publishes events
- [ ] CloudEvents published to Kafka with correct types
- [ ] No references to com.paklog.domain.* packages
- [ ] No references to com.paklog.integration.* packages
- [ ] Documentation updated (README.md)

---

## Rollback Plan

If issues occur during migration:

```bash
# Revert all changes
git checkout main
git branch -D decouple/remove-shared-dependencies

# Or revert specific files
git checkout main -- pom.xml
git checkout main -- src/

# Restore dependencies in pom.xml
# Re-add paklog-domain and paklog-integration dependencies
```

---

## Post-Decoupling Tasks

After successful deployment:

1. **Monitor Production**
   - Track event publishing metrics
   - Monitor outbox table size
   - Check for failed events

2. **Performance Validation**
   - Measure event latency (target: <100ms)
   - Monitor outbox polling overhead
   - Check database query performance

3. **Documentation**
   - Update architecture diagrams
   - Document event schemas in API docs
   - Update deployment runbook

4. **Team Communication**
   - Notify downstream services of event schema ownership
   - Share CloudEvents type definitions
   - Update service catalog

---

## Success Criteria

- ✅ Zero dependencies on paklog-domain
- ✅ Zero dependencies on paklog-integration
- ✅ Service builds in < 2 minutes independently
- ✅ All tests passing with ≥80% coverage
- ✅ Events published with correct CloudEvents types
- ✅ Production deployment successful with zero errors
- ✅ Monitoring shows healthy event publishing

---

## Support & Questions

For questions or issues during decoupling:
- Consult main plan: `/MICROSERVICES_DECOUPLING_PLAN.md`
- Review outbox pattern: `src/main/java/com/paklog/wave/planning/infrastructure/outbox/`
- Check CloudEvents spec: https://cloudevents.io/

**Estimated Total Time:** 8 hours
**Complexity:** HIGH
**Risk Level:** MEDIUM (Outbox pattern must work correctly)
