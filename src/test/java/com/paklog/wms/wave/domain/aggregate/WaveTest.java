package com.paklog.wms.wave.domain.aggregate;

import com.paklog.domain.shared.DomainEvent;
import com.paklog.wms.wave.domain.event.WaveCancelledEvent;
import com.paklog.wms.wave.domain.event.WaveCompletedEvent;
import com.paklog.wms.wave.domain.event.WavePlannedEvent;
import com.paklog.wms.wave.domain.event.WaveReleasedEvent;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStatus;
import com.paklog.wms.wave.domain.valueobject.WaveStrategy;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaveTest {

    @Test
    void planInitializesWaveAndRegistersEvent() {
        Wave wave = newPlannedWave("WAVE-PLAN", List.of("ORD-1", "ORD-2"));

        assertThat(wave.getStatus()).isEqualTo(WaveStatus.PLANNED);
        assertThat(wave.getOrderIds()).containsExactly("ORD-1", "ORD-2");
        assertThat(wave.getPriority()).isEqualTo(WavePriority.NORMAL);
        assertThat(wave.getMetrics().getTotalOrders()).isEqualTo(2);
        assertThat(wave.getDomainEvents())
                .singleElement()
                .isInstanceOf(WavePlannedEvent.class);
    }

    @Test
    void planRejectsEmptyOrders() {
        Wave wave = new Wave();
        wave.setWaveId("WAVE-EMPTY");

        assertThatThrownBy(() -> wave.plan(List.of(), defaultStrategy(), "WH-1", WavePriority.HIGH, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wave must contain at least one order");
    }

    @Test
    void releaseRequiresInventoryAndZone() {
        Wave wave = newPlannedWave();
        wave.assignZone("ZONE-A");

        assertThatThrownBy(wave::release)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inventory allocation");
    }

    @Test
    void releaseTransitionsWaveAndRegistersEvent() {
        Wave wave = newPlannedWave("WAVE-REL", List.of("ORD-1", "ORD-2"));
        wave.assignZone("ZONE-A");
        wave.markInventoryAllocated();

        wave.release();

        assertThat(wave.getStatus()).isEqualTo(WaveStatus.RELEASED);
        assertThat(wave.getActualReleaseTime()).isNotNull();
        assertThat(lastEvent(wave)).isInstanceOf(WaveReleasedEvent.class);
    }

    @Test
    void startExecutionRequiresReleasedStatus() {
        Wave wave = newPlannedWave();

        assertThatThrownBy(wave::startExecution)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Wave must be in RELEASED status");
    }

    @Test
    void startExecutionTransitionsFromReleased() {
        Wave wave = readyForExecutionWave();
        wave.release();

        wave.startExecution();

        assertThat(wave.getStatus()).isEqualTo(WaveStatus.IN_PROGRESS);
    }

    @Test
    void completeRequiresInProgressStatus() {
        Wave wave = readyForExecutionWave();

        assertThatThrownBy(wave::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IN_PROGRESS");
    }

    @Test
    void completeUpdatesMetricsAndRegistersEvent() {
        Wave wave = readyForExecutionWave();
        wave.release();
        wave.startExecution();

        wave.complete();

        assertThat(wave.getStatus()).isEqualTo(WaveStatus.COMPLETED);
        assertThat(wave.getMetrics().getActualPickTime()).isNotNull();
        assertThat(lastEvent(wave)).isInstanceOf(WaveCompletedEvent.class);
    }

    @Test
    void cancelTransitionsToCancelledAndRegistersEvent() {
        Wave wave = newPlannedWave();

        wave.cancel("No inventory");

        assertThat(wave.getStatus()).isEqualTo(WaveStatus.CANCELLED);
        assertThat(lastEvent(wave)).isInstanceOf(WaveCancelledEvent.class);
    }

    @Test
    void cancelFailsForTerminalStates() {
        Wave wave = readyForExecutionWave();
        wave.release();
        wave.startExecution();
        wave.complete();

        assertThatThrownBy(() -> wave.cancel("Too late"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a COMPLETED wave");
    }

    @Test
    void assignZoneOnlyWhenPlanned() {
        Wave wave = readyForExecutionWave();
        wave.release();
        wave.startExecution();

        assertThatThrownBy(() -> wave.assignZone("ZONE-B"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("planned waves");
    }

    @Test
    void addOrdersValidatesCapacity() {
        Wave wave = newPlannedWave("WAVE-CAP", List.of("ORD-1"));

        wave.addOrders(List.of("ORD-2"));
        assertThat(wave.getOrderIds()).containsExactly("ORD-1", "ORD-2");

        assertThatThrownBy(() -> wave.addOrders(List.of("ORD-3", "ORD-4")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceed max wave capacity");
    }

    @Test
    void removeOrdersUpdatesMetrics() {
        Wave wave = newPlannedWave("WAVE-REM", List.of("ORD-1", "ORD-2"));

        wave.removeOrders(List.of("ORD-2"));

        assertThat(wave.getOrderIds()).containsExactly("ORD-1");
        assertThat(wave.getMetrics().getTotalOrders()).isEqualTo(1);
    }

    @Test
    void markInventoryAllocatedOnlyWhenPlanned() {
        Wave wave = readyForExecutionWave();
        wave.release();

        assertThatThrownBy(wave::markInventoryAllocated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("planned waves");
    }

    private Wave newPlannedWave() {
        return newPlannedWave("WAVE-100", List.of("ORD-1", "ORD-2"));
    }

    private Wave newPlannedWave(String id, List<String> orderIds) {
        Wave wave = new Wave();
        wave.setWaveId(id);
        wave.plan(orderIds, defaultStrategy(), "WH-1", null, LocalDateTime.now().plusMinutes(30));
        return wave;
    }

    private Wave readyForExecutionWave() {
        Wave wave = newPlannedWave();
        wave.assignZone("ZONE-A");
        wave.markInventoryAllocated();
        return wave;
    }

    private WaveStrategy defaultStrategy() {
        return WaveStrategy.builder()
                .type(WaveStrategyType.TIME_BASED)
                .maxOrders(3)
                .maxLines(10)
                .timeInterval(Duration.ofMinutes(30))
                .build();
    }

    private DomainEvent lastEvent(Wave wave) {
        List<DomainEvent> events = wave.getDomainEvents();
        return events.get(events.size() - 1);
    }
}
