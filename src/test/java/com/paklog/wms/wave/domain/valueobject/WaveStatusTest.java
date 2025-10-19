package com.paklog.wms.wave.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WaveStatusTest {

    @Test
    @DisplayName("PLANNED should transition to RELEASED")
    void plannedShouldTransitionToReleased() {
        assertTrue(WaveStatus.PLANNED.canTransitionTo(WaveStatus.RELEASED));
    }

    @Test
    @DisplayName("PLANNED should transition to CANCELLED")
    void plannedShouldTransitionToCancelled() {
        assertTrue(WaveStatus.PLANNED.canTransitionTo(WaveStatus.CANCELLED));
    }

    @Test
    @DisplayName("PLANNED should not transition to IN_PROGRESS")
    void plannedShouldNotTransitionToInProgress() {
        assertFalse(WaveStatus.PLANNED.canTransitionTo(WaveStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("RELEASED should transition to IN_PROGRESS")
    void releasedShouldTransitionToInProgress() {
        assertTrue(WaveStatus.RELEASED.canTransitionTo(WaveStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("IN_PROGRESS should transition to COMPLETED")
    void inProgressShouldTransitionToCompleted() {
        assertTrue(WaveStatus.IN_PROGRESS.canTransitionTo(WaveStatus.COMPLETED));
    }

    @Test
    @DisplayName("COMPLETED should not transition to any status")
    void completedShouldNotTransitionToAnyStatus() {
        assertFalse(WaveStatus.COMPLETED.canTransitionTo(WaveStatus.PLANNED));
        assertFalse(WaveStatus.COMPLETED.canTransitionTo(WaveStatus.RELEASED));
        assertFalse(WaveStatus.COMPLETED.canTransitionTo(WaveStatus.IN_PROGRESS));
        assertFalse(WaveStatus.COMPLETED.canTransitionTo(WaveStatus.CANCELLED));
    }

    @Test
    @DisplayName("CANCELLED should not transition to any status")
    void cancelledShouldNotTransitionToAnyStatus() {
        assertFalse(WaveStatus.CANCELLED.canTransitionTo(WaveStatus.PLANNED));
        assertFalse(WaveStatus.CANCELLED.canTransitionTo(WaveStatus.RELEASED));
    }

    @Test
    @DisplayName("Should identify terminal statuses")
    void shouldIdentifyTerminalStatuses() {
        assertTrue(WaveStatus.COMPLETED.isTerminal());
        assertTrue(WaveStatus.CANCELLED.isTerminal());
        assertFalse(WaveStatus.PLANNED.isTerminal());
        assertFalse(WaveStatus.RELEASED.isTerminal());
        assertFalse(WaveStatus.IN_PROGRESS.isTerminal());
    }

    @Test
    @DisplayName("Should identify active statuses")
    void shouldIdentifyActiveStatuses() {
        assertTrue(WaveStatus.RELEASED.isActive());
        assertTrue(WaveStatus.IN_PROGRESS.isActive());
        assertFalse(WaveStatus.PLANNED.isActive());
        assertFalse(WaveStatus.COMPLETED.isActive());
        assertFalse(WaveStatus.CANCELLED.isActive());
    }
}
