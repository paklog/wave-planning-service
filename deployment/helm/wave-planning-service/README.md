# Wave Planning Service Helm Chart

Intelligent wave planning with multi-strategy optimization

## Installation

```bash
helm install wave-planning-service .
```

## Configuration

See `values.yaml` for all configuration options.

### Key Configuration

```yaml
image:
  repository: paklog/wave-planning-service
  tag: latest

service:
  port: 8100
  managementPort: 8081

resources:
  limits:
    cpu: 1000m
    memory: 1Gi
  requests:
    cpu: 500m
    memory: 512Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
```

### Database Configuration

MongoDB is enabled by default.



### Observability

- **Metrics**: Prometheus metrics available at `/actuator/prometheus`
- **Tracing**: OpenTelemetry traces sent to Tempo
- **Logging**: Structured logs sent to Loki

## Uninstallation

```bash
helm uninstall wave-planning-service
```

## Maintainers

- Paklog Team
