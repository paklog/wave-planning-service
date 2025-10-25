package com.paklog.wms.wave.infrastructure.shadow;

import com.paklog.wms.wave.infrastructure.featureflags.FeatureFlagService;
import com.paklog.wms.wave.infrastructure.resilience.WarehouseOperationsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShadowModeServiceTest {

    @Mock
    private FeatureFlagService featureFlagService;

    @Mock
    private WarehouseOperationsClient legacyClient;

    @Mock
    private ShadowModeMetrics metrics;

    private ShadowModeService shadowModeService;

    @BeforeEach
    void setUp() {
        shadowModeService = new ShadowModeService(featureFlagService, legacyClient, metrics);
    }

    @Test
    void executeShadowCallReturnsEarlyWhenDisabled() {
        when(featureFlagService.isShadowModeEnabled("WAVE-1")).thenReturn(false);

        CompletableFuture<ShadowModeService.ShadowModeResult> resultFuture =
                shadowModeService.executeShadowCall("WAVE-1", Map.of("status", "new"));

        ShadowModeService.ShadowModeResult result = resultFuture.join();
        assertThat(result.matches()).isTrue();
        assertThat(result.message()).isEqualTo("Shadow mode disabled");

        verify(metrics, never()).recordShadowExecution();
    }

    @Test
    void executeShadowCallRecordsMatchForEqualResults() {
        when(featureFlagService.isShadowModeEnabled("WAVE-2")).thenReturn(true);
        when(legacyClient.planWave("WAVE-2")).thenReturn(Map.of("status", "ok"));

        Map<String, Object> newResult = Map.of("status", "ok");
        ShadowModeService.ShadowModeResult result = shadowModeService
                .executeShadowCall("WAVE-2", newResult)
                .join();

        assertThat(result.matches()).isTrue();
        verify(metrics).recordShadowExecution();
        verify(metrics).recordMatch();
    }

    @Test
    void executeShadowCallRecordsMismatch() {
        when(featureFlagService.isShadowModeEnabled("WAVE-3")).thenReturn(true);
        when(legacyClient.planWave("WAVE-3")).thenReturn(Map.of("status", "legacy"));

        ShadowModeService.ShadowModeResult result = shadowModeService
                .executeShadowCall("WAVE-3", Map.of("status", "new"))
                .join();

        assertThat(result.matches()).isFalse();
        verify(metrics).recordShadowExecution();
        verify(metrics).recordMismatch();
    }

    @Test
    void executeShadowCallRecordsErrorsFromLegacySystem() {
        when(featureFlagService.isShadowModeEnabled("WAVE-4")).thenReturn(true);
        when(legacyClient.planWave("WAVE-4")).thenThrow(new RuntimeException("legacy down"));

        ShadowModeService.ShadowModeResult result = shadowModeService
                .executeShadowCall("WAVE-4", Map.of("status", "new"))
                .join();

        assertThat(result.matches()).isFalse();
        assertThat(result.message()).contains("Error");
        verify(metrics).recordShadowExecution();
        verify(metrics).recordError();
        verify(metrics, never()).recordMatch();
        verify(metrics, never()).recordMismatch();
    }
}

