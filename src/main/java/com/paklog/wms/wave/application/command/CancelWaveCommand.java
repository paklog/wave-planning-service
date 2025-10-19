package com.paklog.wms.wave.application.command;

/**
 * Command to cancel a wave
 */
public record CancelWaveCommand(
        String waveId,
        String reason
) {
    public CancelWaveCommand {
        if (waveId == null || waveId.isBlank()) {
            throw new IllegalArgumentException("Wave ID cannot be null or empty");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or empty");
        }
    }
}
