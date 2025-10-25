package com.paklog.wms.wave.infrastructure.featureflags;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureFlagServiceTest {

    private FeatureFlagService featureFlagService;

    @BeforeEach
    void setUp() {
        featureFlagService = new FeatureFlagService();
    }

    @Test
    void shadowModeDisabledReturnsFalse() {
        ReflectionTestUtils.setField(featureFlagService, "shadowModeEnabled", false);
        ReflectionTestUtils.setField(featureFlagService, "shadowModePercentage", 100);

        assertThat(featureFlagService.isShadowModeEnabled()).isFalse();
        assertThat(featureFlagService.isShadowModeEnabled("WAVE-1")).isFalse();
    }

    @Test
    void fullPercentageReturnsTrue() {
        ReflectionTestUtils.setField(featureFlagService, "shadowModeEnabled", true);
        ReflectionTestUtils.setField(featureFlagService, "shadowModePercentage", 100);

        assertThat(featureFlagService.isShadowModeEnabled()).isTrue();
    }

    @Test
    void partialPercentageUsesRandomSampling() {
        ReflectionTestUtils.setField(featureFlagService, "shadowModeEnabled", true);
        ReflectionTestUtils.setField(featureFlagService, "shadowModePercentage", 10);

        Random fixedRandom = new Random() {
            @Override
            public int nextInt(int bound) {
                return 5; // always lower than configured percentage
            }
        };
        ReflectionTestUtils.setField(featureFlagService, "random", fixedRandom);

        assertThat(featureFlagService.isShadowModeEnabled()).isTrue();
    }

    @Test
    void isFeatureEnabledCoversKnownAndUnknownFlags() {
        ReflectionTestUtils.setField(featureFlagService, "shadowModeEnabled", true);
        ReflectionTestUtils.setField(featureFlagService, "reconciliationEnabled", false);

        assertThat(featureFlagService.isFeatureEnabled("shadow-mode")).isTrue();
        assertThat(featureFlagService.isFeatureEnabled("reconciliation")).isFalse();
        assertThat(featureFlagService.isFeatureEnabled("unknown-feature")).isFalse();
    }
}

