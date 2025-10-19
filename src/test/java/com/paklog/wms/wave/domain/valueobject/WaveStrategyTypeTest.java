package com.paklog.wms.wave.domain.valueobject;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class WaveStrategyTypeTest {

    @ParameterizedTest
    @EnumSource(WaveStrategyType.class)
    void descriptionIsProvidedForEveryType(WaveStrategyType type) {
        assertThat(type.getDescription()).isNotBlank();
    }
}
