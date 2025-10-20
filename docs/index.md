---
layout: default
title: Home
---

# Wave Planning Service Documentation

Warehouse picking wave creation and optimization service with advanced multi-strategy algorithms and real-time event processing.

## Overview

The Wave Planning Service is responsible for creating, optimizing, and managing warehouse picking waves. It implements sophisticated algorithms to group orders efficiently based on various strategies including carrier cutoffs, zone optimization, and capacity constraints. The service uses Domain-Driven Design principles with event-driven architecture to integrate seamlessly with other warehouse operations.

## Quick Links

### Getting Started
- [README](README.md) - Quick start guide and overview
- [Architecture Overview](architecture.md) - System architecture description

### Architecture & Design
- [Domain Model](DOMAIN-MODEL.md) - Complete domain model with class diagrams
- [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) - Process flows and interactions
- [OpenAPI Specification](openapi.yaml) - REST API documentation
- [AsyncAPI Specification](asyncapi.yaml) - Event documentation

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.2** - Application framework
- **MongoDB** - Document database for wave storage
- **Apache Kafka** - Event streaming platform
- **CloudEvents 2.5.0** - Event standard
- **Maven** - Build tool

## Key Features

- **Multi-Strategy Wave Creation** - Time-based, carrier-based, zone-based, capacity-based
- **Wave Optimization** - Multi-objective optimization for travel distance, workload, and SLA
- **Real-time Event Processing** - Reacts to order events and inventory changes
- **Carrier Cutoff Management** - Ensures orders meet shipping deadlines
- **Zone-based Optimization** - Groups orders by warehouse zones
- **Capacity Management** - Respects picker and volume constraints

## Domain Model

### Aggregates
- **Wave** - Complete wave lifecycle management with optimization

### Entities
- **Order** - Order references for wave planning
- **WaveMetrics** - Wave performance metrics

### Value Objects
- **CarrierCutoff** - Carrier deadline information
- **WaveCapacity** - Wave capacity constraints
- **Location** - Physical location data
- **Coordinates** - 3D position tracking

### Wave Lifecycle

```
DRAFT -> PLANNED -> RELEASED -> IN_PROGRESS -> COMPLETED
                              \-> CANCELLED
```

## Domain Events

### Published Events
- **WaveCreated** - New wave created
- **WaveOptimized** - Wave optimization completed
- **WaveReleased** - Wave released to warehouse
- **WaveStarted** - Wave execution started
- **WaveCompleted** - Wave execution completed
- **WaveCancelled** - Wave cancelled
- **OrderAddedToWave** - Order added to wave
- **OrderRemovedFromWave** - Order removed from wave

### Consumed Events
- **FulfillmentOrderValidated** - Orders available for waving
- **InventoryReserved** - Inventory confirmed for orders

## Architecture Patterns

- **Hexagonal Architecture** - Ports and adapters for clean separation
- **Domain-Driven Design** - Rich domain model with business logic
- **Event-Driven Architecture** - Asynchronous integration via events
- **Strategy Pattern** - Multiple wave creation strategies
- **Repository Pattern** - Data access abstraction

## API Endpoints

### Wave Management
- `POST /waves` - Create new wave
- `GET /waves/{waveId}` - Get wave details
- `PUT /waves/{waveId}/optimize` - Optimize wave
- `POST /waves/{waveId}/release` - Release wave to warehouse
- `POST /waves/{waveId}/cancel` - Cancel wave
- `GET /waves` - List waves with filtering

### Wave Operations
- `POST /waves/{waveId}/orders/{orderId}` - Add order to wave
- `DELETE /waves/{waveId}/orders/{orderId}` - Remove order from wave
- `GET /waves/{waveId}/metrics` - Get wave metrics

## Wave Strategies

### Time-Based Strategy
Groups orders by fixed time windows with configurable start time and window duration.

### Carrier-Based Strategy
Groups orders by carrier cutoff times to ensure on-time shipments.

### Zone-Based Strategy
Groups orders by warehouse zones to minimize picker travel.

### Capacity-Based Strategy
Groups orders respecting wave capacity constraints for orders, lines, volume, and weight.

### Priority-Based Strategy
Prioritizes urgent orders and customer SLAs in wave creation.

## Integration Points

### Consumes Events From
- Order Management (order validated)
- Inventory Service (inventory reserved)
- Task Execution (wave progress)

### Publishes Events To
- Task Execution (wave released)
- Workload Planning (wave planned)
- Pick Execution (wave started)

## Performance Considerations

- Wave optimization runs asynchronously for large waves (>50 orders)
- Metrics are cached and updated incrementally
- Zone data is preloaded for optimization algorithms
- Distance calculations use Manhattan distance for speed
- MongoDB indexes on warehouse, status, and planned times

## Business Rules

1. **Wave Creation Rules**
   - Minimum 10 orders per wave (configurable)
   - Maximum 100 orders per wave (capacity-based)
   - Cannot exceed picker capacity
   - Must respect carrier cutoff times

2. **Optimization Constraints**
   - Balance travel distance vs. SLA priority
   - Group orders by zone when possible
   - Respect carrier service level agreements
   - Maintain picker workload balance

3. **Capacity Management**
   - Volume capacity: 1000 cubic feet default
   - Weight capacity: 5000 lbs default
   - Line capacity: 500 lines default
   - Picker capacity: Based on availability

## Getting Started

1. Review the [README](README.md) for quick start instructions
2. Understand the [Architecture](architecture.md) and design patterns
3. Explore the [Domain Model](DOMAIN-MODEL.md) to understand business concepts
4. Study the [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) for process flows
5. Reference the [OpenAPI](openapi.yaml) and [AsyncAPI](asyncapi.yaml) specifications

## Configuration

Key configuration properties:
- `wave.min-orders` - Minimum orders per wave
- `wave.max-orders` - Maximum orders per wave
- `wave.optimization.max-iterations` - Optimization algorithm iterations
- `wave.capacity.volume` - Maximum wave volume
- `wave.capacity.weight` - Maximum wave weight

## Contributing

For contribution guidelines, please refer to the main README in the project root.

## Support

- **GitHub Issues**: Report bugs or request features
- **Documentation**: Browse the guides in the navigation menu
- **Service Owner**: WMS Team
- **Slack**: #wms-wave-planning
