package com.paklog.wms.wave.application.service;

import com.paklog.domain.shared.DomainEvent;
import com.paklog.wms.wave.application.command.AssignZoneCommand;
import com.paklog.wms.wave.application.command.CancelWaveCommand;
import com.paklog.wms.wave.application.command.CreateWaveCommand;
import com.paklog.wms.wave.application.command.ReleaseWaveCommand;
import com.paklog.wms.wave.application.service.WavePlanningService.WaveNotFoundException;
import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.repository.WaveRepository;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStatus;
import com.paklog.wms.wave.domain.valueobject.WaveStrategy;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import com.paklog.wms.wave.infrastructure.events.WaveEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WavePlanningServiceTest {

    @Mock
    private WaveRepository waveRepository;

    @Mock
    private WaveEventPublisher eventPublisher;

    @InjectMocks
    private WavePlanningService wavePlanningService;

    @BeforeEach
    void setupRepository() {
        lenient().when(waveRepository.save(any(Wave.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createWavePersistsAndPublishesEvents() {
        CreateWaveCommand command = new CreateWaveCommand(
                List.of("ORD-1", "ORD-2"),
                WaveStrategyType.TIME_BASED,
                "WH-1",
                WavePriority.HIGH,
                LocalDateTime.now().plusHours(1),
                10,
                20,
                Duration.ofMinutes(30)
        );

        Wave created = wavePlanningService.createWave(command);

        assertThat(created.getWaveId()).isNotNull();
        verify(waveRepository).save(any(Wave.class));

        ArgumentCaptor<List<DomainEvent>> eventCaptor = ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publishEvents(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).anyMatch(event -> event.getClass().getSimpleName().equals("WavePlannedEvent"));
    }

    @Test
    void releaseWavePublishesEvents() {
        Wave wave = plannedWave();
        wave.assignZone("A1");
        wave.markInventoryAllocated();
        when(waveRepository.findById(eq(wave.getWaveId()))).thenReturn(Optional.of(wave));

        Wave released = wavePlanningService.releaseWave(new ReleaseWaveCommand(wave.getWaveId()));

        assertThat(released.getStatus()).isEqualTo(WaveStatus.RELEASED);
        verify(eventPublisher).publishEvents(anyList());
        verify(waveRepository, times(1)).save(any(Wave.class));
    }

    @Test
    void releaseWaveThrowsWhenNotFound() {
        when(waveRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wavePlanningService.releaseWave(new ReleaseWaveCommand("unknown")))
                .isInstanceOf(WaveNotFoundException.class);
    }

    @Test
    void cancelWavePublishesEvent() {
        Wave wave = plannedWave();
        when(waveRepository.findById(wave.getWaveId())).thenReturn(Optional.of(wave));

        Wave cancelled = wavePlanningService.cancelWave(new CancelWaveCommand(wave.getWaveId(), "No capacity"));

        assertThat(cancelled.getStatus()).isEqualTo(WaveStatus.CANCELLED);
        verify(eventPublisher).publishEvents(anyList());
    }

    @Test
    void assignZonePersistsChanges() {
        Wave wave = plannedWave();
        when(waveRepository.findById(wave.getWaveId())).thenReturn(Optional.of(wave));

        Wave updated = wavePlanningService.assignZone(new AssignZoneCommand(wave.getWaveId(), "B2"));

        assertThat(updated.getAssignedZone()).isEqualTo("B2");
        verify(waveRepository, times(1)).save(any(Wave.class));
    }

    @Test
    void findOperationsDelegateToRepository() {
        Wave wave = plannedWave();
        when(waveRepository.findById("id")).thenReturn(Optional.of(wave));

        wavePlanningService.findWaveById("id");
        verify(waveRepository).findById("id");

        wavePlanningService.findWavesByWarehouseAndStatus("WH-1", WaveStatus.PLANNED);
        verify(waveRepository).findByWarehouseIdAndStatus("WH-1", WaveStatus.PLANNED);

        wavePlanningService.findActiveWaves();
        verify(waveRepository).findActiveWaves();

        wavePlanningService.findWavesReadyToRelease();
        verify(waveRepository).findReadyToRelease(eq(WaveStatus.PLANNED), any(LocalDateTime.class));
    }

    private Wave plannedWave() {
        Wave wave = new Wave();
        wave.setWaveId("WAVE-TEST");
        wave.plan(
                List.of("ORD-1", "ORD-2"),
                WaveStrategy.builder()
                        .type(WaveStrategyType.TIME_BASED)
                        .maxOrders(10)
                        .timeInterval(Duration.ofMinutes(15))
                        .build(),
                "WH-1",
                WavePriority.NORMAL,
                LocalDateTime.now().plusMinutes(20)
        );
        return wave;
    }
}
