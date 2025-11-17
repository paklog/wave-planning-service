# Wave Planning Service

Intelligent warehouse wave creation and optimization service with multi-strategy algorithms, event-driven architecture, and hexagonal design.

## Overview

The Wave Planning Service orchestrates the creation, optimization, and management of warehouse picking waves within the Paklog platform. This bounded context groups fulfillment orders into efficient waves based on various strategies including carrier cutoffs, zone optimization, and capacity constraints. It publishes domain events to integrate with downstream services like Task Execution, Pick Execution, and Workload Planning.

## Domain-Driven Design

### Bounded Context
**Wave Planning & Optimization** - Manages the lifecycle of warehouse waves from creation through completion with intelligent grouping and optimization algorithms.

### Core Domain Model

#### Aggregates
- **Wave** - Root aggregate representing a collection of orders for coordinated picking

#### Entities
- **Order** - Fulfillment order assigned to a wave
- **WaveMetrics** - Performance metrics for wave execution

#### Value Objects
- **WaveStatus** - Wave status enumeration (DRAFT, PLANNED, RELEASED, IN_PROGRESS, COMPLETED, CANCELLED)
- **WaveType** - Wave strategy type (TIME_BASED, CARRIER_BASED, ZONE_BASED, CAPACITY_BASED, PRIORITY_BASED)
- **CarrierCutoff** - Carrier shipping deadline configuration
- **WaveCapacity** - Wave size constraints
- **Location** - Warehouse location reference
- **OptimizationCriteria** - Multi-objective optimization parameters

#### Domain Events
- **WaveCreatedEvent** - New wave created
- **WaveOptimizedEvent** - Wave optimization completed
- **WaveReleasedEvent** - Wave released for execution
- **WaveStartedEvent** - Wave picking started
- **WaveCompletedEvent** - Wave execution completed
- **WaveCancelledEvent** - Wave cancelled
- **WavesMergedEvent** - Multiple waves merged

### Ubiquitous Language
- **Wave**: Coordinated batch of orders for efficient warehouse picking
- **Carrier Cutoff**: Deadline for shipping via specific carrier
- **Zone Optimization**: Grouping orders by warehouse zones to minimize travel
- **Wave Capacity**: Maximum orders, lines, volume, or weight per wave
- **Wave Release**: Making wave available to warehouse for execution
- **Travel Distance**: Total distance pickers must travel to complete wave
- **Multi-Objective Optimization**: Balancing multiple factors (distance, SLA, workload)

## Architecture & Patterns

### Hexagonal Architecture (Ports and Adapters)

```
src/main/java/com/paklog/wms/wave/
├── domain/                           # Core business logic
│   ├── aggregate/                   # Aggregates
│   │   └── Wave.java                # Wave aggregate root
│   ├── entity/                      # Entities
│   │   ├── Order.java               # Order entity
│   │   └── WaveMetrics.java         # Metrics entity
│   ├── valueobject/                 # Value objects
│   │   ├── CarrierCutoff.java       # Carrier deadline
│   │   ├── WaveCapacity.java        # Capacity constraints
│   │   ├── Location.java            # Location reference
│   │   └── OptimizationCriteria.java # Optimization params
│   ├── service/                     # Domain services
│   │   ├── WaveOptimizationService.java # Optimization algorithms
│   │   └── WaveAllocationService.java   # Resource allocation
│   ├── repository/                  # Repository interfaces (ports)
│   └── event/                       # Domain events
├── application/                      # Use cases & orchestration
│   ├── service/                     # Application services
│   ├── command/                     # Commands
│   ├── query/                       # Queries
│   └── port/                        # Application ports
└── infrastructure/                   # External adapters
    ├── persistence/                 # MongoDB repositories
    ├── messaging/                   # Kafka publishers
    ├── web/                         # REST controllers
    └── config/                      # Configuration
```

### Design Patterns & Principles
- **Hexagonal Architecture** - Clean separation of domain and infrastructure
- **Domain-Driven Design** - Rich domain model with business invariants
- **Strategy Pattern** - Multiple wave creation strategies
- **Factory Pattern** - Wave creation based on type
- **Event-Driven Architecture** - Integration via domain events
- **Aggregate Pattern** - Consistency boundaries around Wave
- **Repository Pattern** - Data access abstraction
- **SOLID Principles** - Maintainable and extensible code

### Optimization Algorithms
- **Multi-Objective Optimization** - Weighted scoring for multiple criteria
- **Greedy Nearest Neighbor** - Travel distance optimization
- **Zone-Based Clustering** - Minimize zone transitions
- **Capacity Bin Packing** - Efficient capacity utilization
- **Priority Sorting** - SLA and customer tier prioritization

## Technology Stack

### Core Framework
- **Java 21** - Programming language
- **Spring Boot 3.2** - Application framework with GraalVM Native Image support
- **Maven** - Build and dependency management

### Data & Persistence
- **MongoDB** - Document database for aggregates
- **Spring Data MongoDB** - Data access layer

### Messaging & Events
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration
- **CloudEvents** - Standardized event format

### API & Documentation
- **Spring Web MVC** - REST API framework
- **Bean Validation** - Input validation
- **OpenAPI/Swagger** - API documentation

### Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **Micrometer Tracing** - Distributed tracing
- **Structured Logging** - JSON log format

### Testing
- **JUnit 5** - Unit testing framework
- **Testcontainers** - Integration testing
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Local development environment

## Standards Applied

### Architectural Standards
- ✅ Hexagonal Architecture (Ports and Adapters)
- ✅ Domain-Driven Design tactical patterns
- ✅ Event-Driven Architecture
- ✅ Microservices architecture
- ✅ RESTful API design
- ✅ Strategy pattern for wave creation

### Algorithm Standards
- ✅ Multi-objective optimization (distance, SLA, workload)
- ✅ Zone-based optimization
- ✅ Capacity constraint satisfaction
- ✅ Carrier cutoff compliance
- ✅ Performance metrics tracking

### Code Quality Standards
- ✅ SOLID principles
- ✅ Clean Code practices
- ✅ Comprehensive unit and integration testing
- ✅ Domain-driven design patterns
- ✅ Immutable value objects
- ✅ Rich domain models with business logic

### Event & Integration Standards
- ✅ CloudEvents specification v1.0
- ✅ Event-driven integration
- ✅ At-least-once delivery semantics
- ✅ Event versioning strategy
- ✅ Idempotent event handling

### Observability Standards
- ✅ Structured logging (JSON)
- ✅ Distributed tracing
- ✅ Health check endpoints
- ✅ Prometheus metrics
- ✅ Correlation ID propagation

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/paklog/wave-planning-service.git
   cd wave-planning-service
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d mongodb kafka
   ```

3. **Build and run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify the service is running**
   ```bash
   curl http://localhost:8081/actuator/health
   ```

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f wave-planning-service

# Stop all services
docker-compose down
```

### GraalVM Native Image Build

For improved startup time (~0.2s vs ~6s) and reduced memory footprint (~150MB vs ~600MB):

```bash
# Build native executable (requires GraalVM)
./mvnw -Pnative clean package -DskipTests

# Run native executable
./target/wave-planning-service
```

Or build as a Docker image:

```bash
# Build native Docker image
docker build -f Dockerfile.native -t wave-planning-service:native .

# Run native container
docker run -p 8080:8080 \
  -e MONGODB_URI=mongodb://host.docker.internal:27017/wave_planning \
  -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  wave-planning-service:native
```

For detailed native image build instructions, see [NATIVE_BUILD.md](NATIVE_BUILD.md).

## API Documentation

Once running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8081/v3/api-docs

### Key Endpoints

- `POST /waves` - Create new wave
- `GET /waves/{waveId}` - Get wave by ID
- `POST /waves/create-carrier-based` - Create carrier-based waves
- `POST /waves/create-zone-based` - Create zone-based waves
- `POST /waves/create-capacity-based` - Create capacity-based waves
- `POST /waves/{waveId}/optimize` - Optimize wave
- `POST /waves/{waveId}/release` - Release wave for execution
- `GET /waves/{waveId}/progress` - Get wave progress
- `POST /waves/merge` - Merge multiple waves

## Wave Creation Strategies

### Carrier-Based Waves
Groups orders by carrier cutoff times to meet shipping deadlines:
```java
// Groups: FedEx (4pm cutoff), UPS (6pm cutoff), USPS (8pm cutoff)
List<Wave> carrierWaves = createCarrierWaves(orders, cutoffs);
```

### Zone-Based Waves
Groups orders by warehouse zones to minimize picker travel:
```java
// Creates waves with max 3 zones per wave
List<Wave> zoneWaves = createZoneWaves(orders, warehouseId);
```

### Capacity-Based Waves
Creates waves respecting capacity constraints:
```java
// Max 100 orders, 500 lines, 1000 cubic feet per wave
List<Wave> capacityWaves = createCapacityWaves(orders, capacity);
```

### Time-Based Waves
Creates waves in fixed time windows:
```java
// 2-hour windows starting at 8am
List<Wave> timeWaves = createTimeBasedWaves(orders, windowSize);
```

## Optimization Features

### Multi-Objective Optimization
- **Travel Distance** (30% weight) - Minimize picker travel
- **SLA Compliance** (40% weight) - Prioritize urgent orders
- **Workload Balance** (30% weight) - Even distribution across pickers

### Performance Metrics
- Average improvement: 20-30% over unoptimized waves
- Processing time: <200ms for 100 orders
- Optimization iterations: Max 100 (configurable)

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run tests with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Configuration

Key configuration properties:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/wave_planning
  kafka:
    bootstrap-servers: localhost:9092

wave-planning:
  optimization:
    max-iterations: 100
    improvement-threshold: 0.01
  capacity:
    default-max-orders: 100
    default-max-lines: 500
    default-max-volume: 1000.0
    default-max-weight: 5000.0
  carrier:
    cutoff-buffer-minutes: 30
```

## Event Integration

### Published Events
- `com.paklog.wave.created.v1`
- `com.paklog.wave.optimized.v1`
- `com.paklog.wave.released.v1`
- `com.paklog.wave.started.v1`
- `com.paklog.wave.completed.v1`
- `com.paklog.wave.cancelled.v1`
- `com.paklog.waves.merged.v1`

### Consumed Events
- `com.paklog.order.created` - New orders to plan
- `com.paklog.order.priority.changed` - Priority updates
- `com.paklog.order.cancelled` - Order cancellations
- `com.paklog.pick.session.completed` - Wave progress tracking
- `com.paklog.carrier.cutoff.approaching` - Cutoff warnings

## Wave Lifecycle

```
DRAFT → PLANNED → RELEASED → IN_PROGRESS → COMPLETED
           ↓          ↓            ↓
      CANCELLED   CANCELLED    CANCELLED
```

## Monitoring

- **Health**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/metrics
- **Prometheus**: http://localhost:8081/actuator/prometheus
- **Info**: http://localhost:8081/actuator/info

### Key Metrics
- `wave.creation.time` - Wave creation duration
- `wave.optimization.improvement` - Optimization percentage
- `wave.size.orders` - Orders per wave
- `wave.completion.rate` - Wave success rate

## Contributing

1. Follow hexagonal architecture principles
2. Implement domain logic in domain layer
3. Maintain aggregate consistency boundaries
4. Use appropriate wave creation strategy
5. Ensure optimization algorithms are tested
6. Write comprehensive tests including algorithm tests
7. Document optimization strategies
8. Follow existing code style and conventions

## License

Copyright © 2024 Paklog. All rights reserved.