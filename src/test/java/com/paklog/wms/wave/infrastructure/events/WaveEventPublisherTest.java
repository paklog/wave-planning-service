package com.paklog.wms.wave.infrastructure.events;

import com.paklog.wave.planning.infrastructure.outbox.OutboxEvent;
import com.paklog.wave.planning.infrastructure.outbox.OutboxService;
import com.paklog.wms.wave.domain.event.WavePlannedEvent;
import com.paklog.wms.wave.domain.event.WaveReleasedEvent;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaveEventPublisherTest {

    @Mock
    private OutboxService outboxService;

    private WaveEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new WaveEventPublisher(outboxService);
        when(outboxService.saveEvent(any(), any(), any())).thenReturn(new OutboxEvent());
    }

    @Test
    void publishEventSavesEventInOutbox() {
        WavePlannedEvent event = new WavePlannedEvent(
                "WAVE-1",
                List.of("ORD-1"),
                "WH-1",
                WaveStrategyType.TIME_BASED,
                WavePriority.CRITICAL,
                LocalDateTime.now()
        );

        publisher.publishEvent(event);

        ArgumentCaptor<String> aggregateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).saveEvent(aggregateCaptor.capture(), eventTypeCaptor.capture(), eq(event));

        assertThat(aggregateCaptor.getValue()).isEqualTo("WAVE-1");
        assertThat(eventTypeCaptor.getValue()).isEqualTo("com.paklog.wms.wave.wave.planned.v1");
    }

    @Test
    void publishEventsPublishesAllDomainEvents() {
        WaveReleasedEvent releasedEvent = new WaveReleasedEvent(
                "WAVE-2",
                List.of("ORD-3"),
                "WH-2",
                "ZONE-A",
                WavePriority.HIGH
        );
        WavePlannedEvent plannedEvent = new WavePlannedEvent(
                "WAVE-2",
                List.of("ORD-3"),
                "WH-2",
                WaveStrategyType.CAPACITY_BASED,
                WavePriority.HIGH,
                LocalDateTime.now()
        );

        publisher.publishEvents(List.of(releasedEvent, plannedEvent));

        verify(outboxService, times(2)).saveEvent(eq("WAVE-2"), any(), any());
    }

    @Test
    void publishEventWrapsOutboxFailures() {
        when(outboxService.saveEvent(any(), any(), any()))
                .thenThrow(new RuntimeException("outbox down"));

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
                .hasMessageContaining("Failed to save event to outbox");
    }
}
