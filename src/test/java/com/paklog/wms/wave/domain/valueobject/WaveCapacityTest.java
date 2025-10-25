package com.paklog.wms.wave.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WaveCapacityTest {

    @Test
    void builderConfiguresAllFields() {
        WaveCapacity capacity = WaveCapacity.builder()
                .maxOrders(50)
                .maxLines(200)
                .maxVolume(new BigDecimal("2500.5"))
                .maxWeight(new BigDecimal("1200.0"))
                .maxPickers(8)
                .build();

        assertThat(capacity.getMaxOrders()).isEqualTo(50);
        assertThat(capacity.getMaxLines()).isEqualTo(200);
        assertThat(capacity.getMaxVolume()).isEqualByComparingTo("2500.5");
        assertThat(capacity.getMaxWeight()).isEqualByComparingTo("1200.0");
        assertThat(capacity.getMaxPickers()).isEqualTo(8);
    }

    @Test
    void equalityComparesAllFields() {
        WaveCapacity capacity1 = WaveCapacity.builder().maxOrders(20).build();
        WaveCapacity capacity2 = WaveCapacity.builder().maxOrders(20).build();

        assertThat(capacity1).isEqualTo(capacity2);
        assertThat(capacity1.hashCode()).isEqualTo(capacity2.hashCode());
    }
}

