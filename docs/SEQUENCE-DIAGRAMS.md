# Wave Planning Service - Sequence Diagrams

## 1. Wave Creation and Optimization Flow

### Create Carrier-Based Wave

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant WaveController
    participant WaveService
    participant WaveOptimizationService
    participant OrderService
    participant WaveRepository
    participant EventPublisher
    participant Kafka

    Client->>WaveController: POST /waves/create-carrier-based
    Note over Client: Request includes carrier cutoffs

    WaveController->>WaveService: createCarrierWaves(request)

    WaveService->>OrderService: findPendingOrders()
    OrderService-->>WaveService: List<Order>

    WaveService->>WaveOptimizationService: createCarrierWaves(orders, cutoffs)

    loop For each carrier cutoff
        WaveOptimizationService->>WaveOptimizationService: filterOrdersByCarrier()
        WaveOptimizationService->>WaveOptimizationService: sortByPriority()
        WaveOptimizationService->>WaveOptimizationService: createWave()
        WaveOptimizationService->>WaveOptimizationService: calculateMetrics()
    end

    WaveOptimizationService-->>WaveService: List<Wave>

    loop For each wave
        WaveService->>WaveRepository: save(wave)
        WaveRepository-->>WaveService: Wave (saved)

        WaveService->>EventPublisher: publish(WaveCreated)
        EventPublisher->>Kafka: send(wave.created)
    end

    WaveService-->>WaveController: List<Wave>
    WaveController-->>Client: 200 OK (waves)
```

### Optimize Existing Wave

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant WaveController
    participant WaveService
    participant WaveOptimizationService
    participant TSPSolver
    participant WaveRepository
    participant EventPublisher

    Client->>WaveController: POST /waves/{id}/optimize
    Note over Client: Optimization criteria in body

    WaveController->>WaveService: optimizeWave(waveId, criteria)

    WaveService->>WaveRepository: findById(waveId)
    WaveRepository-->>WaveService: Wave

    WaveService->>WaveService: validateWaveStatus()
    Note over WaveService: Must be PLANNED status

    WaveService->>WaveOptimizationService: optimizeWave(wave, orders, criteria)

    alt Minimize Travel Distance
        WaveOptimizationService->>TSPSolver: optimizePickSequence(orders)
        TSPSolver-->>WaveOptimizationService: OptimizedSequence
    end

    alt Balance Workload
        WaveOptimizationService->>WaveOptimizationService: distributeByLines()
        WaveOptimizationService->>WaveOptimizationService: balanceByTime()
    end

    alt Prioritize SLA
        WaveOptimizationService->>WaveOptimizationService: sortBySLA()
        WaveOptimizationService->>WaveOptimizationService: groupByPriority()
    end

    WaveOptimizationService->>WaveOptimizationService: calculateOptimizedMetrics()
    WaveOptimizationService-->>WaveService: OptimizedWave

    WaveService->>WaveRepository: save(optimizedWave)
    WaveRepository-->>WaveService: Wave (saved)

    WaveService->>EventPublisher: publish(WaveOptimized)
    EventPublisher-->>WaveService: Published

    WaveService-->>WaveController: OptimizedWave
    WaveController-->>Client: 200 OK (wave with metrics)
```

## 2. Wave Release and Execution Flow

### Release Wave for Picking

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant WaveController
    participant WaveService
    participant AllocationService
    participant TaskService
    participant WaveRepository
    participant EventPublisher
    participant TaskExecutionService
    participant PickExecutionService

    Client->>WaveController: POST /waves/{id}/release

    WaveController->>WaveService: releaseWave(waveId)

    WaveService->>WaveRepository: findById(waveId)
    WaveRepository-->>WaveService: Wave

    WaveService->>WaveService: validateCanRelease()
    Note over WaveService: Check status is PLANNED

    WaveService->>AllocationService: allocatePickers(wave)
    AllocationService->>AllocationService: calculateRequiredPickers()
    AllocationService->>AllocationService: findAvailablePickers()
    AllocationService-->>WaveService: PickerAllocation

    WaveService->>WaveService: updateStatus(RELEASED)
    WaveService->>WaveService: setReleaseTime()

    WaveService->>WaveRepository: save(wave)
    WaveRepository-->>WaveService: Wave (updated)

    WaveService->>EventPublisher: publish(WaveReleased)
    EventPublisher->>TaskExecutionService: wave.released event
    EventPublisher->>PickExecutionService: wave.released event

    par Create Pick Tasks
        TaskExecutionService->>TaskExecutionService: createPickTasks(wave)
        TaskExecutionService->>TaskExecutionService: enqueueTasks()
    and Create Pick Sessions
        PickExecutionService->>PickExecutionService: createPickSessions(wave)
        PickExecutionService->>PickExecutionService: optimizePaths()
    end

    WaveService-->>WaveController: ReleasedWave
    WaveController-->>Client: 200 OK (wave released)
```

### Monitor Wave Progress

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant WaveController
    participant WaveMonitoringService
    participant WaveRepository
    participant TaskService
    participant PickService
    participant MetricsCalculator

    Client->>WaveController: GET /waves/{id}/progress

    WaveController->>WaveMonitoringService: getWaveProgress(waveId)

    WaveMonitoringService->>WaveRepository: findById(waveId)
    WaveRepository-->>WaveMonitoringService: Wave

    par Gather Progress Data
        WaveMonitoringService->>TaskService: getTaskProgress(waveId)
        TaskService-->>WaveMonitoringService: TaskProgress
    and
        WaveMonitoringService->>PickService: getPickProgress(waveId)
        PickService-->>WaveMonitoringService: PickProgress
    end

    WaveMonitoringService->>MetricsCalculator: calculateProgress(tasks, picks)
    MetricsCalculator->>MetricsCalculator: aggregateMetrics()
    MetricsCalculator-->>WaveMonitoringService: WaveProgress

    WaveMonitoringService->>WaveMonitoringService: calculateETA()
    WaveMonitoringService->>WaveMonitoringService: identifyBottlenecks()

    WaveMonitoringService-->>WaveController: WaveProgressReport
    WaveController-->>Client: 200 OK (progress details)
```

## 3. Zone-Based Wave Creation Flow

```mermaid
sequenceDiagram
    autonumber
    participant Scheduler
    participant WaveService
    participant WaveOptimizationService
    participant OrderService
    participant LocationService
    participant ZoneAnalyzer
    participant WaveRepository

    Scheduler->>WaveService: createZoneBasedWaves()
    Note over Scheduler: Triggered by schedule or API

    WaveService->>OrderService: findPendingOrders()
    OrderService-->>WaveService: List<Order>

    WaveService->>WaveOptimizationService: createZoneWaves(orders, warehouseId)

    WaveOptimizationService->>LocationService: getWarehouseZones(warehouseId)
    LocationService-->>WaveOptimizationService: List<Zone>

    loop For each order
        WaveOptimizationService->>ZoneAnalyzer: analyzeOrderZones(order)
        ZoneAnalyzer->>ZoneAnalyzer: extractPickLocations()
        ZoneAnalyzer->>ZoneAnalyzer: mapToZones()
        ZoneAnalyzer-->>WaveOptimizationService: ZoneDistribution
    end

    WaveOptimizationService->>WaveOptimizationService: groupOrdersByZone()
    WaveOptimizationService->>WaveOptimizationService: optimizeZoneCombinations()

    loop For each zone group
        WaveOptimizationService->>WaveOptimizationService: createWave()
        WaveOptimizationService->>WaveOptimizationService: setZoneMetadata()
        WaveOptimizationService->>WaveOptimizationService: calculateZoneMetrics()
    end

    WaveOptimizationService-->>WaveService: List<Wave>

    loop For each wave
        WaveService->>WaveRepository: save(wave)
        WaveRepository-->>WaveService: Wave (saved)
    end

    WaveService-->>Scheduler: WaveCreationResult
```

## 4. Wave Cancellation Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant WaveController
    participant WaveService
    participant WaveRepository
    participant TaskService
    participant PickService
    participant OrderService
    participant EventPublisher

    Client->>WaveController: DELETE /waves/{id}
    Note over Client: With cancellation reason

    WaveController->>WaveService: cancelWave(waveId, reason)

    WaveService->>WaveRepository: findById(waveId)
    WaveRepository-->>WaveService: Wave

    WaveService->>WaveService: validateCanCancel()
    Note over WaveService: Cannot cancel COMPLETED waves

    alt Wave is RELEASED or IN_PROGRESS
        par Cancel Tasks
            WaveService->>TaskService: cancelWaveTasks(waveId)
            TaskService->>TaskService: findTasksByWave()
            TaskService->>TaskService: cancelTasks()
            TaskService-->>WaveService: CancellationResult
        and Cancel Pick Sessions
            WaveService->>PickService: cancelPickSessions(waveId)
            PickService->>PickService: findSessionsByWave()
            PickService->>PickService: cancelSessions()
            PickService-->>WaveService: CancellationResult
        end
    end

    WaveService->>OrderService: releaseOrders(wave.orderIds)
    OrderService->>OrderService: updateOrderStatus(PENDING)
    OrderService-->>WaveService: OrdersReleased

    WaveService->>WaveService: updateStatus(CANCELLED)
    WaveService->>WaveService: setCancellationReason(reason)

    WaveService->>WaveRepository: save(wave)
    WaveRepository-->>WaveService: Wave (cancelled)

    WaveService->>EventPublisher: publish(WaveCancelled)
    EventPublisher-->>WaveService: Published

    WaveService-->>WaveController: CancellationConfirmation
    WaveController-->>Client: 200 OK (wave cancelled)
```

## 5. Capacity-Based Wave Creation Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant WaveController
    participant WaveService
    participant WaveOptimizationService
    participant CapacityCalculator
    participant OrderService
    participant WaveRepository

    Client->>WaveController: POST /waves/create-capacity-based
    Note over Client: WaveCapacity constraints

    WaveController->>WaveService: createCapacityWaves(capacity)

    WaveService->>OrderService: findPendingOrders()
    OrderService-->>WaveService: List<Order>

    WaveService->>WaveOptimizationService: createCapacityBasedWaves(orders, capacity)

    WaveOptimizationService->>WaveOptimizationService: sortOrdersByPriority()

    loop While orders remain
        WaveOptimizationService->>WaveOptimizationService: createEmptyWave()

        loop For each order
            WaveOptimizationService->>CapacityCalculator: canAccommodate(wave, order, capacity)

            CapacityCalculator->>CapacityCalculator: checkOrderLimit()
            CapacityCalculator->>CapacityCalculator: checkLineLimit()
            CapacityCalculator->>CapacityCalculator: checkVolumeLimit()
            CapacityCalculator->>CapacityCalculator: checkWeightLimit()
            CapacityCalculator->>CapacityCalculator: checkPickerLimit()

            CapacityCalculator-->>WaveOptimizationService: boolean

            alt Can accommodate
                WaveOptimizationService->>WaveOptimizationService: addOrderToWave()
                WaveOptimizationService->>WaveOptimizationService: updateWaveMetrics()
            end
        end

        WaveOptimizationService->>WaveOptimizationService: finalizeWave()
    end

    WaveOptimizationService-->>WaveService: List<Wave>

    loop For each wave
        WaveService->>WaveRepository: save(wave)
        WaveRepository-->>WaveService: Wave (saved)
    end

    WaveService-->>WaveController: List<Wave>
    WaveController-->>Client: 200 OK (waves created)
```

## 6. Event-Driven Wave Updates

```mermaid
sequenceDiagram
    autonumber
    participant OrderService
    participant Kafka
    participant WaveEventHandler
    participant WaveService
    participant WaveRepository
    participant NotificationService

    OrderService->>Kafka: order.priority.changed
    Note over OrderService: High priority order

    Kafka->>WaveEventHandler: consume(OrderPriorityChanged)

    WaveEventHandler->>WaveService: handleOrderPriorityChange(orderId)

    WaveService->>WaveRepository: findWaveByOrder(orderId)
    WaveRepository-->>WaveService: Wave

    alt Order priority increased significantly
        WaveService->>WaveService: evaluateWaveReorganization()

        alt Should reorganize
            WaveService->>WaveService: removeOrderFromWave()
            WaveService->>WaveService: createPriorityWave()
            WaveService->>WaveService: addOrderToPriorityWave()

            WaveService->>WaveRepository: save(waves)

            WaveService->>NotificationService: notifyWaveChange()
            NotificationService->>NotificationService: alertSupervisor()
        end
    end

    WaveService->>Kafka: wave.reorganized
    WaveEventHandler-->>OrderService: Acknowledgment
```

## 7. Wave Merge Operation

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant WaveController
    participant WaveService
    participant WaveOptimizationService
    participant CapacityValidator
    participant WaveRepository
    participant EventPublisher

    Client->>WaveController: POST /waves/merge
    Note over Client: List of wave IDs

    WaveController->>WaveService: mergeWaves(waveIds)

    loop For each waveId
        WaveService->>WaveRepository: findById(waveId)
        WaveRepository-->>WaveService: Wave
    end

    WaveService->>WaveService: validateCanMerge()
    Note over WaveService: All must be PLANNED status

    WaveService->>WaveOptimizationService: canMergeWaves(waves)

    WaveOptimizationService->>CapacityValidator: validateMergedCapacity()
    CapacityValidator->>CapacityValidator: sumMetrics()
    CapacityValidator->>CapacityValidator: checkLimits()
    CapacityValidator-->>WaveOptimizationService: ValidationResult

    alt Can merge
        WaveOptimizationService->>WaveOptimizationService: createMergedWave()
        WaveOptimizationService->>WaveOptimizationService: combineOrders()
        WaveOptimizationService->>WaveOptimizationService: recalculateMetrics()
        WaveOptimizationService->>WaveOptimizationService: optimizeMergedWave()

        WaveOptimizationService-->>WaveService: MergedWave

        WaveService->>WaveRepository: save(mergedWave)

        loop For each original wave
            WaveService->>WaveService: markAsSuperseded()
            WaveService->>WaveRepository: save(wave)
        end

        WaveService->>EventPublisher: publish(WavesMerged)

        WaveService-->>WaveController: MergedWave
        WaveController-->>Client: 200 OK (merged wave)
    else Cannot merge
        WaveService-->>WaveController: MergeError
        WaveController-->>Client: 400 Bad Request (reason)
    end
```

## Error Handling Patterns

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant WaveController
    participant WaveService
    participant ExternalService
    participant ErrorHandler
    participant AlertService

    Client->>WaveController: POST /waves/create

    WaveController->>WaveService: createWave(request)

    WaveService->>ExternalService: fetchRequiredData()

    alt Service Timeout
        ExternalService--x WaveService: Timeout

        WaveService->>ErrorHandler: handleTimeout()
        ErrorHandler->>ErrorHandler: logError()
        ErrorHandler->>AlertService: sendAlert()
        ErrorHandler->>ErrorHandler: applyFallback()
        ErrorHandler-->>WaveService: FallbackData

        WaveService->>WaveService: createWithFallback()
        WaveService-->>WaveController: PartialWave
        WaveController-->>Client: 202 Accepted (partial)

    else Service Error
        ExternalService-->>WaveService: Error Response

        WaveService->>ErrorHandler: handleError()
        ErrorHandler->>ErrorHandler: classifyError()

        alt Recoverable Error
            ErrorHandler->>ErrorHandler: retry()
            ErrorHandler-->>WaveService: RetryResult
        else Non-Recoverable
            ErrorHandler->>AlertService: criticalAlert()
            ErrorHandler-->>WaveService: ErrorDetails
            WaveService-->>WaveController: ErrorResponse
            WaveController-->>Client: 500 Internal Error
        end
    end
```

## Key Interaction Patterns

1. **Asynchronous Processing**: Large wave optimizations are processed asynchronously
2. **Event Sourcing**: All state changes emit events for downstream services
3. **Compensation**: Failed operations trigger compensation transactions
4. **Circuit Breaker**: External service calls use circuit breaker pattern
5. **Retry Logic**: Transient failures are retried with exponential backoff
6. **Idempotency**: All operations are idempotent using request IDs