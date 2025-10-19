package com.paklog.wms.wave.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaveIdTest {

    @Test
    void generateProducesPrefixedId() {
        WaveId generated = WaveId.generate();

        assertThat(generated.getValue()).startsWith("WAVE-");
        assertThat(generated.toString()).isEqualTo(generated.getValue());
    }

    @Test
    void ofValidatesInput() {
        assertThatThrownBy(() -> WaveId.of(" "))
                .isInstanceOf(IllegalArgumentException.class);

        WaveId id = WaveId.of("WAVE-1234");
        assertThat(id.getValue()).isEqualTo("WAVE-1234");
    }

    @Test
    void equalsAndHashCodeBasedOnValue() {
        WaveId first = WaveId.of("WAVE-1234");
        WaveId second = WaveId.of("WAVE-1234");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
