package com.paklog.wms.wave.config;

import com.paklog.wave.planning.infrastructure.outbox.OutboxEvent;
import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.entity.Order;
import com.paklog.wms.wave.domain.entity.WaveMetrics;
import com.paklog.wms.wave.domain.event.WaveCancelledEvent;
import com.paklog.wms.wave.domain.event.WaveCompletedEvent;
import com.paklog.wms.wave.domain.event.WavePlannedEvent;
import com.paklog.wms.wave.domain.event.WaveReleasedEvent;
import com.paklog.wms.wave.domain.valueobject.*;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Registers runtime hints for GraalVM native image compilation.
 * This ensures that all domain classes, events, and value objects
 * that are used with Jackson serialization (Kafka, CloudEvents)
 * are properly registered for reflection.
 */
public class NativeRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Register domain events for reflection (Kafka serialization)
        registerForReflection(hints, WavePlannedEvent.class);
        registerForReflection(hints, WaveReleasedEvent.class);
        registerForReflection(hints, WaveCompletedEvent.class);
        registerForReflection(hints, WaveCancelledEvent.class);
        registerForReflection(hints, OutboxEvent.class);

        // Register domain aggregate
        registerForReflection(hints, Wave.class);

        // Register domain entities
        registerForReflection(hints, Order.class);
        registerForReflection(hints, WaveMetrics.class);

        // Register value objects
        registerForReflection(hints, WaveId.class);
        registerForReflection(hints, WaveStatus.class);
        registerForReflection(hints, WaveStrategy.class);
        registerForReflection(hints, WaveStrategyType.class);
        registerForReflection(hints, WaveCapacity.class);
        registerForReflection(hints, WavePriority.class);

        // Register CloudEvents classes
        try {
            hints.reflection().registerType(
                Class.forName("io.cloudevents.CloudEvent"),
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.DECLARED_FIELDS
            );
            hints.reflection().registerType(
                Class.forName("io.cloudevents.core.v1.CloudEventV1"),
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.DECLARED_FIELDS
            );
        } catch (ClassNotFoundException e) {
            // CloudEvents classes not found, skip registration
        }
    }

    private void registerForReflection(RuntimeHints hints, Class<?> clazz) {
        hints.reflection().registerType(
            clazz,
            MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_PUBLIC_METHODS,
            MemberCategory.INVOKE_DECLARED_METHODS,
            MemberCategory.DECLARED_FIELDS
        );
        hints.serialization().registerType(clazz);
    }
}
