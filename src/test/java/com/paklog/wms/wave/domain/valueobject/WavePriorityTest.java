package com.paklog.wms.wave.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WavePriorityTest {

    @Test
    void comparisonOperationsWorkAsExpected() {
        assertThat(WavePriority.CRITICAL.isHigherThan(WavePriority.HIGH)).isTrue();
        assertThat(WavePriority.LOW.isLowerThan(WavePriority.NORMAL)).isTrue();
    }
}
