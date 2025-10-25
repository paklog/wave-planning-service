package com.paklog.wms.wave.infrastructure.reconciliation;

import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.repository.WaveRepository;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStrategy;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import com.paklog.wms.wave.infrastructure.featureflags.FeatureFlagService;
import com.paklog.wms.wave.infrastructure.resilience.WarehouseOperationsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private WaveRepository waveRepository;

    @Mock
    private WarehouseOperationsClient legacyClient;

    @Mock
    private FeatureFlagService featureFlagService;

    @Mock
    private ReconciliationMetrics reconciliationMetrics;

    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new ReconciliationService(
                waveRepository,
                legacyClient,
                featureFlagService,
                reconciliationMetrics
        );
        ReflectionTestUtils.setField(reconciliationService, "maxVariancePercentage", 10.0);
    }

    @Test
    void runReconciliationSkipsWhenFeatureDisabled() {
        when(featureFlagService.isReconciliationEnabled()).thenReturn(false);

        reconciliationService.runReconciliation();

        verify(featureFlagService).isReconciliationEnabled();
        verify(waveRepository, never()).findWavesCreatedAfter(any());
        verify(reconciliationMetrics, never()).recordReconciliation(any());
    }

    @Test
    void runReconciliationProcessesRecentWaves() {
        when(featureFlagService.isReconciliationEnabled()).thenReturn(true);
        Wave wave = plannedWave("WAVE-1");
        when(waveRepository.findWavesCreatedAfter(any())).thenReturn(List.of(wave));
        when(legacyClient.getWave("WAVE-1")).thenReturn(new Object());

        reconciliationService.runReconciliation();

        ArgumentCaptor<ReconciliationService.ReconciliationReport> reportCaptor =
                ArgumentCaptor.forClass(ReconciliationService.ReconciliationReport.class);
        verify(reconciliationMetrics).recordReconciliation(reportCaptor.capture());

        ReconciliationService.ReconciliationReport report = reportCaptor.getValue();
        assertThat(report.totalChecked()).isEqualTo(1);
        assertThat(report.mismatches()).isEmpty();
        verify(reconciliationMetrics, never()).recordHighVariance();
    }

    @Test
    void runReconciliationRecordsMismatchesAndHighVariance() {
        when(featureFlagService.isReconciliationEnabled()).thenReturn(true);
        Wave wave = plannedWave("WAVE-2");
        when(waveRepository.findWavesCreatedAfter(any())).thenReturn(List.of(wave));
        when(legacyClient.getWave("WAVE-2")).thenReturn(null);
        ReflectionTestUtils.setField(reconciliationService, "maxVariancePercentage", 0.0);

        reconciliationService.runReconciliation();

        verify(reconciliationMetrics).recordHighVariance();
    }

    @Test
    void runReconciliationRecordsErrors() {
        when(featureFlagService.isReconciliationEnabled()).thenReturn(true);
        Wave wave = plannedWave("WAVE-3");
        when(waveRepository.findWavesCreatedAfter(any())).thenReturn(List.of(wave));
        when(legacyClient.getWave("WAVE-3")).thenThrow(new RuntimeException("legacy unavailable"));

        reconciliationService.runReconciliation();

        ArgumentCaptor<ReconciliationService.ReconciliationReport> reportCaptor =
                ArgumentCaptor.forClass(ReconciliationService.ReconciliationReport.class);
        verify(reconciliationMetrics).recordReconciliation(reportCaptor.capture());
        assertThat(reportCaptor.getValue().errors()).isNotEmpty();
    }

    private Wave plannedWave(String id) {
        Wave wave = new Wave();
        wave.setWaveId(id);
        wave.plan(
                List.of("ORD-1", "ORD-2"),
                WaveStrategy.builder()
                        .type(WaveStrategyType.TIME_BASED)
                        .maxOrders(5)
                        .timeInterval(Duration.ofMinutes(5))
                        .build(),
                "WH-1",
                WavePriority.NORMAL,
                LocalDateTime.now()
        );
        return wave;
    }
}
