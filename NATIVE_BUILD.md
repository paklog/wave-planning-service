# GraalVM Native Image Build Guide

This guide explains how to build and run the Wave Planning Service as a GraalVM native image for improved startup time and reduced memory footprint.

## Prerequisites

### Local Build
- **GraalVM 21** or later with Native Image support
- **Maven 3.8+**
- **Minimum 4GB RAM** (8GB recommended for build)
- **Disk space**: ~2GB for build artifacts

### Installing GraalVM

#### Option 1: SDKMAN (Recommended)
```bash
sdk install java 21-graalce
sdk use java 21-graalce
gu install native-image
```

#### Option 2: Direct Download
Download from [GraalVM Releases](https://github.com/graalvm/graalvm-ce-builds/releases) and install `native-image`:
```bash
$GRAALVM_HOME/bin/gu install native-image
```

## Building Native Image

### Option 1: Maven Profile (Recommended)

Build native executable:
```bash
./mvnw -Pnative clean package -DskipTests
```

The native executable will be created at: `target/wave-planning-service`

### Option 2: Docker Multi-stage Build

Build using the native Dockerfile:
```bash
docker build -f Dockerfile.native -t wave-planning-service:native .
```

This creates a minimal ~100MB image (vs ~300MB for JVM-based image).

### Option 3: Spring Boot Maven Plugin

```bash
./mvnw -Pnative spring-boot:build-image
```

This uses Cloud Native Buildpacks to create a container with the native image.

## Running Native Image

### Local Execution
```bash
./target/wave-planning-service
```

### Docker
```bash
docker run -p 8080:8080 \
  -e MONGODB_URI=mongodb://mongo:27017/wave_planning \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  wave-planning-service:native
```

## Performance Characteristics

### Startup Time Comparison
- **JVM**: ~5-8 seconds
- **Native Image**: ~0.1-0.3 seconds (15-50x faster)

### Memory Footprint
- **JVM**: ~400-600MB heap + ~200MB metaspace
- **Native Image**: ~100-200MB total (3-5x smaller)

### Throughput
- **JVM**: Better for long-running, high-throughput applications (C2 JIT compiler)
- **Native Image**: Comparable for moderate loads, excellent for serverless/microservices

## Configuration for Native Image

### Runtime Hints

The application includes `NativeRuntimeHintsRegistrar` which automatically registers:
- Domain events (WavePlannedEvent, WaveReleasedEvent, etc.)
- Domain entities and value objects
- CloudEvents classes
- Kafka serialization classes

Located at: `src/main/java/com/paklog/wms/wave/config/NativeRuntimeHintsRegistrar.java`

### Reflection Configuration

Manual reflection configuration is provided at:
- `src/main/resources/META-INF/native-image/com.paklog.wms/wave-planning-service/reflect-config.json`
- `src/main/resources/META-INF/native-image/com.paklog.wms/wave-planning-service/resource-config.json`

### Build Arguments

The native-maven-plugin is configured with:
- `--no-fallback`: Ensures pure native image without JVM fallback
- `-H:+ReportExceptionStackTraces`: Better error reporting during build
- `--initialize-at-build-time`: Pre-initializes certain classes for faster startup

## Troubleshooting

### Build Failures

**Problem**: `ClassNotFoundException` during native image build

**Solution**: Add missing classes to `NativeRuntimeHintsRegistrar.java` or `reflect-config.json`

**Problem**: Out of memory during build

**Solution**: Increase Docker memory or set Maven opts:
```bash
export MAVEN_OPTS="-Xmx8g"
```

### Runtime Issues

**Problem**: Missing reflection configuration

**Solution**: Run with tracing agent to generate config:
```bash
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image \
  -jar target/wave-planning-service-0.0.1-SNAPSHOT.jar
```

**Problem**: Serialization errors with Kafka/CloudEvents

**Solution**: Ensure all DTO/Event classes are registered in `NativeRuntimeHintsRegistrar`

## Testing Native Image

### Native Tests
Run tests in native mode:
```bash
./mvnw -Pnative test
```

### Integration Testing
```bash
# Start dependencies
docker-compose up -d mongodb kafka

# Run native executable
./target/wave-planning-service

# Test endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api-docs
```

## Known Limitations

1. **Dynamic Reflection**: Requires explicit configuration via RuntimeHints or reflect-config.json
2. **JVMTI/Agents**: Debug agents, profilers may not work
3. **Dynamic Class Loading**: `Class.forName()` requires reflection configuration
4. **Build Time**: 5-10 minutes vs 30 seconds for JVM build

## Kubernetes/Helm Deployment

Update `deployment/helm/wave-planning-service/values.yaml`:

```yaml
image:
  repository: your-registry/wave-planning-service
  tag: native
  pullPolicy: IfNotPresent

resources:
  limits:
    memory: 256Mi  # Much lower than JVM (512Mi)
  requests:
    cpu: 100m
    memory: 128Mi

livenessProbe:
  initialDelaySeconds: 5  # Reduced from 30 (faster startup)

readinessProbe:
  initialDelaySeconds: 3  # Reduced from 20
```

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Build Native Image
  run: ./mvnw -Pnative clean package -DskipTests

- name: Build Docker Image
  run: docker build -f Dockerfile.native -t $IMAGE_NAME:native .

- name: Push to Registry
  run: docker push $IMAGE_NAME:native
```

## When to Use Native Image

### ✅ Good Use Cases
- Microservices with frequent scaling
- Serverless/Lambda functions
- CI/CD environments (faster startup)
- Resource-constrained environments
- Development/testing (fast feedback loop)

### ❌ Not Recommended
- Long-running batch processing (JVM C2 JIT is faster)
- Applications requiring dynamic class loading
- Heavy use of reflection without proper configuration
- When build time is critical

## Additional Resources

- [Spring Native Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/native-image.html)
- [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
- [GraalVM Native Build Tools](https://graalvm.github.io/native-build-tools/)

## Support

For issues specific to native image builds, check:
1. Build logs in `target/native-image/`
2. GraalVM documentation
3. Spring Native GitHub issues
