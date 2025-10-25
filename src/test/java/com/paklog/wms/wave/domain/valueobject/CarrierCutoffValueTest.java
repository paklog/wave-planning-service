package com.paklog.wms.wave.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CarrierCutoffValueTest {

    @Test
    void comparesCutoffTimesAndEquality() {
        LocalDateTime cutoff = LocalDateTime.now().plusHours(3);
        CarrierCutoff carrierCutoff = new CarrierCutoff("DHL", cutoff, "EXPRESS");

        assertThat(carrierCutoff.isBeforeCutoff(LocalDateTime.now())).isTrue();
        assertThat(carrierCutoff.getServiceLevel()).isEqualTo("EXPRESS");

        CarrierCutoff identical = new CarrierCutoff("DHL", cutoff, "EXPRESS");
        CarrierCutoff different = new CarrierCutoff("UPS", cutoff.plusHours(1), "GROUND");

        assertThat(carrierCutoff).isEqualTo(identical);
        assertThat(carrierCutoff).isNotEqualTo(different);
    }
}

