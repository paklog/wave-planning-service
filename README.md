# Wave Planning Service (WMS)

Wave planning and workload management service for strategic warehouse planning.

## Responsibilities

- Wave strategy configuration and planning
- Wave capacity management
- Carrier cutoff time management
- Wave release orchestration
- Workload forecasting and capacity planning
- Labor planning and optimization

## Architecture

```
domain/
├── aggregate/      # Wave, WavePlan, WaveStrategy
├── entity/         # WaveMetrics, CarrierCutoff
├── valueobject/    # WaveId, WaveStatus, Priority
├── service/        # WavePlanningService, CapacityCalculationService
├── repository/     # WaveRepository
└── event/          # WavePlannedEvent, WaveReleasedEvent

application/
├── command/        # CreateWaveCommand, ReleaseWaveCommand
├── query/          # GetWaveStatusQuery, ListPendingWavesQuery
├── saga/           # WaveReleaseSaga
└── handler/        # Event handlers

adapter/
├── rest/           # REST controllers
└── persistence/    # MongoDB repositories

infrastructure/
├── config/         # Spring configurations
├── messaging/      # Kafka publishers/consumers
└── events/         # Event publishing infrastructure
```

## Tech Stack

- Java 21
- Spring Boot 3.2.0
- MongoDB (wave data persistence)
- Apache Kafka (event-driven integration)
- CloudEvents
- OpenAPI/Swagger

## Running the Service

```bash
mvn spring-boot:run
```

## API Documentation

Available at: http://localhost:8080/swagger-ui.html

## Events Published

- `WavePlannedEvent` - When a wave is planned
- `WaveReleasedEvent` - When a wave is released for execution
- `WaveCancelledEvent` - When a wave is cancelled

## Events Consumed

- `FulfillmentOrderValidatedEvent` - From Order Management
- `InventoryAllocatedEvent` - From Inventory Service
