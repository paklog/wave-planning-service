package com.paklog.wms.wave.domain.valueobject;

import com.paklog.domain.valueobject.Priority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WavePriorityTest {

    @Test
    void comparisonOperationsWorkAsExpected() {
        assertThat(WavePriority.CRITICAL.isHigherThan(WavePriority.HIGH)).isTrue();
        assertThat(WavePriority.LOW.isLowerThan(WavePriority.NORMAL)).isTrue();
    }

    @Test
    void convertsToAndFromDomainPriority() {
        WavePriority fromDomain = WavePriority.fromDomainPriority(Priority.HIGH);
        assertThat(fromDomain).isEqualTo(WavePriority.HIGH);

        assertThat(WavePriority.LOW.toDomainPriority()).isEqualTo(Priority.LOW);
    }
}
