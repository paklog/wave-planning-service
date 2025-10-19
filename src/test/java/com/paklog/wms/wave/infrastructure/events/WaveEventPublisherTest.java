package com.paklog.wms.wave.infrastructure.events;

import com.paklog.wms.wave.domain.event.WavePlannedEvent;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaveEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private WaveEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new WaveEventPublisher(kafkaTemplate, "wms-wave-events", "/test/source");
    }

    @Test
    void publishEventBuildsCloudEventWithExpectedType() {
        when(kafkaTemplate.send(eq("wms-wave-events"), any(String.class), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        WavePlannedEvent event = new WavePlannedEvent(
                "WAVE-1",
                List.of("ORD-1"),
                "WH-1",
                WaveStrategyType.TIME_BASED,
                WavePriority.CRITICAL,
                LocalDateTime.now()
        );

        publisher.publishEvent(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<CloudEvent> cloudEventCaptor = ArgumentCaptor.forClass(CloudEvent.class);
        verify(kafkaTemplate).send(eq("wms-wave-events"), any(String.class), cloudEventCaptor.capture());

        CloudEvent cloudEvent = cloudEventCaptor.getValue();
        assertThat(cloudEvent.getType()).isEqualTo("com.paklog.wms.wave.wave.planned.v1");
        assertThat(cloudEvent.getSource().toString()).isEqualTo("/test/source");
    }

    @Test
    void publishEventWrapsExceptions() {
        when(kafkaTemplate.send(eq("wms-wave-events"), any(String.class), any()))
                .thenThrow(new RuntimeException("kafka down"));

        WavePlannedEvent event = new WavePlannedEvent(
                "WAVE-ERROR",
                List.of("ORD-1"),
                "WH-1",
                WaveStrategyType.TIME_BASED,
                WavePriority.NORMAL,
                LocalDateTime.now()
        );

        assertThatThrownBy(() -> publisher.publishEvent(event))
                .isInstanceOf(WaveEventPublisher.EventPublishException.class)
                .hasMessageContaining("Failed to publish event");
    }
}
