package com.paklog.wms.wave.application.service;

import com.paklog.integration.outbox.OutboxEvent;
import com.paklog.integration.outbox.OutboxService;
import com.paklog.wms.wave.application.command.AssignZoneCommand;
import com.paklog.wms.wave.application.command.CreateWaveCommand;
import com.paklog.wms.wave.application.command.ReleaseWaveCommand;
import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.repository.WaveRepository;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStatus;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import com.paklog.wms.wave.infrastructure.events.WaveEventPublisher;
import com.paklog.wms.wave.support.TestMongoConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@DataMongoTest
@Import(TestMongoConfig.class)
class WavePlanningServiceIntegrationTest {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.5");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private WaveRepository waveRepository;

    private OutboxService outboxService;
    private WaveEventPublisher eventPublisher;
    private WavePlanningService wavePlanningService;

    @BeforeEach
    void setUp() {
        waveRepository.deleteAll();
        outboxService = Mockito.mock(OutboxService.class);
        when(outboxService.saveEvent(any(), any(), any())).thenReturn(new OutboxEvent());
        eventPublisher = new WaveEventPublisher(outboxService);
        wavePlanningService = new WavePlanningService(waveRepository, eventPublisher);
    }

    @Test
    void createWavePersistsAggregateAndStoresOutboxEvent() {
        CreateWaveCommand command = new CreateWaveCommand(
                List.of("ORD-1", "ORD-2"),
                WaveStrategyType.TIME_BASED,
                "WH-TEST",
                WavePriority.HIGH,
                LocalDateTime.now().plusMinutes(30),
                20,
                40,
                Duration.ofMinutes(15)
        );

        Wave saved = wavePlanningService.createWave(command);

        Wave persisted = waveRepository.findById(saved.getWaveId()).orElseThrow();
        assertThat(persisted.getOrderIds()).containsExactly("ORD-1", "ORD-2");
        assertThat(persisted.getStatus()).isEqualTo(WaveStatus.PLANNED);

        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).saveEvent(eq(saved.getWaveId()), eventTypeCaptor.capture(), any());
        assertThat(eventTypeCaptor.getValue()).isEqualTo("com.paklog.wms.wave.wave.planned.v1");
    }

    @Test
    void releaseWaveTransitionsAggregateAndPublishesReleaseEvent() {
        CreateWaveCommand command = new CreateWaveCommand(
                List.of("ORD-10"),
                WaveStrategyType.CAPACITY_BASED,
                "WH-REL",
                WavePriority.NORMAL,
                LocalDateTime.now().minusMinutes(5),
                10,
                null,
                Duration.ofMinutes(10)
        );

        Wave created = wavePlanningService.createWave(command);
        wavePlanningService.assignZone(new AssignZoneCommand(created.getWaveId(), "ZONE-9"));

        Wave stored = waveRepository.findById(created.getWaveId()).orElseThrow();
        stored.markInventoryAllocated();
        waveRepository.save(stored);

        clearInvocations(outboxService);

        wavePlanningService.releaseWave(new ReleaseWaveCommand(created.getWaveId()));

        Wave released = waveRepository.findById(created.getWaveId()).orElseThrow();
        assertThat(released.getStatus()).isEqualTo(WaveStatus.RELEASED);

        ArgumentCaptor<String> eventTypes = ArgumentCaptor.forClass(String.class);
        verify(outboxService, atLeastOnce()).saveEvent(eq(created.getWaveId()), eventTypes.capture(), any());
        assertThat(eventTypes.getAllValues()).contains("com.paklog.wms.wave.wave.released.v1");
    }

    @Test
    void wavesReadyToReleaseReflectRepositoryState() {
        Wave readyWave = new Wave();
        readyWave.setWaveId("WAVE-READY");
        readyWave.plan(
                List.of("ORD-READY"),
                com.paklog.wms.wave.domain.valueobject.WaveStrategy.builder()
                        .type(WaveStrategyType.TIME_BASED)
                        .maxOrders(5)
                        .timeInterval(Duration.ofMinutes(10))
                        .build(),
                "WH-READY",
                WavePriority.NORMAL,
                LocalDateTime.now().minusMinutes(2)
        );
        readyWave.assignZone("ZONE-R");
        readyWave.markInventoryAllocated();
        waveRepository.save(readyWave);

        List<Wave> result = wavePlanningService.findWavesReadyToRelease();

        assertThat(result).extracting(Wave::getWaveId).contains("WAVE-READY");
    }
}
