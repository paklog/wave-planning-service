# Wave Planning Service - Domain Model

## Overview

The Wave Planning Service domain model implements sophisticated warehouse wave management using Domain-Driven Design (DDD) principles. The model centers around the Wave Aggregate, which orchestrates the grouping and optimization of orders for efficient picking.

## Class Diagram

```mermaid
classDiagram
    class Wave {
        +String waveId
        +String warehouseId
        +WaveStatus status
        +WaveType type
        +LocalDateTime createdAt
        +LocalDateTime plannedStartTime
        +LocalDateTime plannedEndTime
        +LocalDateTime actualStartTime
        +LocalDateTime actualEndTime
        +List~String~ orderIds
        +WaveMetrics metrics
        +Map~String Object~ metadata
        +planWave()
        +releaseWave()
        +startExecution()
        +completeExecution()
        +cancelWave()
        +addOrder(orderId)
        +removeOrder(orderId)
        +optimizeWave(criteria)
    }

    class WaveMetrics {
        +int totalOrders
        +int totalLines
        +int totalUnits
        +double totalVolume
        +double totalWeight
        +int estimatedPickers
        +long estimatedDuration
        +double totalDistance
        +Map~String Integer~ ordersByZone
        +Map~String Integer~ ordersByCarrier
        +calculateMetrics()
        +updateProgress()
    }

    class WaveStatus {
        <<enumeration>>
        DRAFT
        PLANNED
        RELEASED
        IN_PROGRESS
        COMPLETED
        CANCELLED
    }

    class WaveType {
        <<enumeration>>
        TIME_BASED
        CARRIER_BASED
        ZONE_BASED
        CAPACITY_BASED
        PRIORITY_BASED
    }

    class Order {
        +String orderId
        +String customerId
        +OrderPriority priority
        +LocalDateTime orderDate
        +LocalDateTime requiredDate
        +String shippingMethod
        +String carrier
        +Address shippingAddress
        +List~OrderLine~ lines
        +OrderStatus status
        +calculateVolume()
        +calculateWeight()
        +getPickZones()
    }

    class OrderLine {
        +String lineId
        +String productId
        +String sku
        +int quantity
        +String pickLocation
        +String zone
        +double weight
        +double volume
    }

    class CarrierCutoff {
        +String carrier
        +LocalDateTime cutoffTime
        +String serviceLevel
        +int priorityScore
        +boolean isExpired()
        +long minutesUntilCutoff()
    }

    class WaveCapacity {
        +int maxOrders
        +int maxLines
        +BigDecimal maxVolume
        +BigDecimal maxWeight
        +int maxPickers
        +long maxDuration
        +boolean canAccommodate(Order)
        +double getUtilization()
    }

    class WaveStrategy {
        <<interface>>
        +List~Wave~ createWaves(orders)
        +Wave optimizeWave(wave, criteria)
        +boolean canMergeWaves(wave1, wave2)
    }

    class TimeBasedStrategy {
        +Duration windowSize
        +LocalTime startTime
        +int minOrders
        +createWaves(orders)
        +optimizeWave(wave, criteria)
    }

    class CarrierBasedStrategy {
        +List~CarrierCutoff~ cutoffs
        +boolean groupByService
        +createWaves(orders)
        +optimizeWave(wave, criteria)
    }

    class ZoneBasedStrategy {
        +String warehouseId
        +int maxZonesPerWave
        +boolean allowCrossZone
        +createWaves(orders)
        +optimizeWave(wave, criteria)
    }

    class CapacityBasedStrategy {
        +WaveCapacity capacity
        +boolean strictEnforcement
        +createWaves(orders)
        +optimizeWave(wave, criteria)
    }

    class OptimizationCriteria {
        +boolean minimizeTravelDistance
        +boolean balanceWorkload
        +boolean prioritizeSLA
        +boolean groupByZone
        +boolean groupByCarrier
        +Map~String Double~ weights
        +double calculateScore(wave)
    }

    Wave "1" *-- "1" WaveMetrics : contains
    Wave "1" --> "1" WaveStatus : has
    Wave "1" --> "1" WaveType : has
    Wave "0..*" o-- "0..*" Order : groups
    Order "1" *-- "1..*" OrderLine : contains
    Wave ..> OptimizationCriteria : uses
    Wave ..> WaveStrategy : uses
    WaveStrategy <|-- TimeBasedStrategy : implements
    WaveStrategy <|-- CarrierBasedStrategy : implements
    WaveStrategy <|-- ZoneBasedStrategy : implements
    WaveStrategy <|-- CapacityBasedStrategy : implements
    CarrierBasedStrategy --> CarrierCutoff : uses
    CapacityBasedStrategy --> WaveCapacity : uses
```

## Entity Relationships

```mermaid
erDiagram
    WAVE ||--o{ ORDER : contains
    WAVE ||--|| WAVE_METRICS : has
    ORDER ||--o{ ORDER_LINE : has
    ORDER_LINE }o--|| PRODUCT : references
    ORDER_LINE }o--|| LOCATION : picks_from
    LOCATION }o--|| ZONE : belongs_to
    WAVE }o--|| WAREHOUSE : belongs_to
    ORDER }o--|| CUSTOMER : placed_by
    ORDER }o--|| CARRIER : ships_via

    WAVE {
        string wave_id PK
        string warehouse_id FK
        string status
        string type
        timestamp planned_start
        timestamp planned_end
        json order_ids
        json metrics
    }

    ORDER {
        string order_id PK
        string customer_id FK
        string priority
        timestamp order_date
        timestamp required_date
        string carrier FK
        string status
    }

    ORDER_LINE {
        string line_id PK
        string order_id FK
        string product_id FK
        int quantity
        string pick_location FK
        string zone FK
    }

    WAVE_METRICS {
        string wave_id FK
        int total_orders
        int total_lines
        double total_volume
        double total_weight
        int estimated_pickers
        long estimated_duration
    }
```

## Value Objects

### Location
```java
public class Location {
    private String aisle;
    private String bay;
    private String level;
    private String position;
    private Coordinates coordinates;
}
```

### Coordinates
```java
public class Coordinates {
    private double x;
    private double y;
    private double z;
    private double distanceTo(Coordinates other);
}
```

### Address
```java
public class Address {
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
```

## Domain Events

```mermaid
classDiagram
    class WaveEvent {
        <<abstract>>
        +String waveId
        +String warehouseId
        +Instant timestamp
        +String userId
    }

    class WaveCreated {
        +WaveType type
        +int orderCount
        +LocalDateTime plannedStart
    }

    class WaveOptimized {
        +OptimizationCriteria criteria
        +double improvementPercent
        +WaveMetrics beforeMetrics
        +WaveMetrics afterMetrics
    }

    class WaveReleased {
        +List~String~ orderIds
        +int assignedPickers
        +LocalDateTime releaseTime
    }

    class WaveStarted {
        +LocalDateTime actualStart
        +List~String~ activeOperators
    }

    class WaveCompleted {
        +LocalDateTime completionTime
        +WaveMetrics finalMetrics
        +long actualDuration
    }

    class WaveCancelled {
        +String reason
        +List~String~ affectedOrders
    }

    class OrderAddedToWave {
        +String orderId
        +String previousWaveId
    }

    class OrderRemovedFromWave {
        +String orderId
        +String reason
    }

    WaveEvent <|-- WaveCreated
    WaveEvent <|-- WaveOptimized
    WaveEvent <|-- WaveReleased
    WaveEvent <|-- WaveStarted
    WaveEvent <|-- WaveCompleted
    WaveEvent <|-- WaveCancelled
    WaveEvent <|-- OrderAddedToWave
    WaveEvent <|-- OrderRemovedFromWave
```

## Aggregates and Boundaries

### Wave Aggregate
- **Root**: Wave
- **Entities**: Order (referenced)
- **Value Objects**: WaveMetrics, CarrierCutoff, WaveCapacity, Location, Address
- **Invariants**:
  - A wave can only be in one status at a time
  - Orders can only belong to one active wave
  - Wave capacity constraints must be respected
  - Cannot modify a completed or cancelled wave

### Consistency Boundaries
- Wave aggregate ensures internal consistency
- Order references are by ID only (no direct object references)
- Cross-aggregate operations use domain events
- Eventual consistency between Wave and Order aggregates

## Domain Services

### WaveOptimizationService
Primary service for wave creation and optimization:
- `optimizeWave()` - Multi-objective optimization
- `createCarrierWaves()` - Group by carrier cutoffs
- `createZoneWaves()` - Group by warehouse zones
- `createCapacityWaves()` - Respect capacity constraints
- `createTimeBasedWaves()` - Fixed time windows

### WaveAllocationService
Handles wave-to-resource allocation:
- `allocatePickers()` - Assign operators to waves
- `calculateRequiredResources()` - Resource planning
- `balanceWorkload()` - Distribute work evenly

### WaveMonitoringService
Real-time wave tracking:
- `trackProgress()` - Monitor completion status
- `calculateETA()` - Estimate completion time
- `detectBottlenecks()` - Identify slow zones
- `recommendAdjustments()` - Suggest optimizations

## Repository Interfaces

```java
public interface WaveRepository {
    Wave findById(String waveId);
    List<Wave> findByWarehouse(String warehouseId);
    List<Wave> findByStatus(WaveStatus status);
    List<Wave> findActiveWaves();
    Wave save(Wave wave);
    void delete(String waveId);
}

public interface OrderRepository {
    Order findById(String orderId);
    List<Order> findByIds(List<String> orderIds);
    List<Order> findPendingOrders();
    List<Order> findByCarrier(String carrier);
    List<Order> findByZone(String zone);
}
```

## Business Rules

1. **Wave Creation Rules**
   - Minimum 10 orders per wave (configurable)
   - Maximum 100 orders per wave (capacity-based)
   - Cannot exceed picker capacity
   - Must respect carrier cutoff times

2. **Wave Status Transitions**
   ```
   DRAFT -> PLANNED -> RELEASED -> IN_PROGRESS -> COMPLETED
                  \-> CANCELLED
   ```

3. **Optimization Constraints**
   - Balance travel distance vs. SLA priority
   - Group orders by zone when possible
   - Respect carrier service level agreements
   - Maintain picker workload balance

4. **Capacity Management**
   - Volume capacity: 1000 cubic feet default
   - Weight capacity: 5000 lbs default
   - Line capacity: 500 lines default
   - Picker capacity: Based on availability

## Performance Considerations

- Wave optimization runs asynchronously for large waves (>50 orders)
- Metrics are cached and updated incrementally
- Zone data is preloaded for optimization algorithms
- Distance calculations use Manhattan distance for speed