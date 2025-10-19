package com.paklog.wms.wave.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarrierCutoffTest {

    @Test
    void constructorInitializesFields() {
        CarrierCutoff cutoff = new CarrierCutoff("CARR-1", "Carrier One", LocalTime.of(15, 30), "EXPRESS");

        assertThat(cutoff.getCarrierId()).isEqualTo("CARR-1");
        assertThat(cutoff.getCarrierName()).isEqualTo("Carrier One");
        assertThat(cutoff.getCutoffTime()).isEqualTo(LocalTime.of(15, 30));
        assertThat(cutoff.getServiceLevel()).isEqualTo("EXPRESS");
        assertThat(cutoff.getActive()).isTrue();
    }

    @Test
    void constructorRejectsNullValues() {
        assertThatThrownBy(() -> new CarrierCutoff(null, "Carrier", LocalTime.NOON, "STANDARD"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CarrierCutoff("C1", null, LocalTime.NOON, "STANDARD"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CarrierCutoff("C1", "Carrier", null, "STANDARD"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void cutoffChecksRespectTimes() {
        CarrierCutoff cutoff = new CarrierCutoff("CARR-2", "Carrier Two", LocalTime.of(17, 0), "STANDARD");

        assertThat(cutoff.isCutoffPassed(LocalTime.of(18, 0))).isTrue();
        assertThat(cutoff.isCutoffPassed(LocalTime.of(16, 0))).isFalse();

        assertThat(cutoff.isCutoffApproaching(LocalTime.of(16, 45), 30)).isTrue();
        assertThat(cutoff.isCutoffApproaching(LocalTime.of(16, 10), 15)).isFalse();
    }

    @Test
    void activationTogglesState() {
        CarrierCutoff cutoff = new CarrierCutoff();
        cutoff.deactivate();
        assertThat(cutoff.getActive()).isFalse();

        cutoff.activate();
        assertThat(cutoff.getActive()).isTrue();
    }

    @Test
    void equalsAndHashCodeUseCarrierAndServiceLevel() {
        CarrierCutoff first = new CarrierCutoff("C1", "Carrier", LocalTime.NOON, "EXPRESS");
        CarrierCutoff second = new CarrierCutoff("C1", "Other", LocalTime.of(13, 0), "EXPRESS");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());

        CarrierCutoff different = new CarrierCutoff("C1", "Carrier", LocalTime.NOON, "STANDARD");
        assertThat(first).isNotEqualTo(different);
    }
}
