package com.paklog.wms.wave.application.command;

/**
 * Command to release a wave for execution
 */
public record ReleaseWaveCommand(
        String waveId,
        boolean force
) {
    public ReleaseWaveCommand {
        if (waveId == null || waveId.isBlank()) {
            throw new IllegalArgumentException("Wave ID cannot be null or empty");
        }
    }

    public ReleaseWaveCommand(String waveId) {
        this(waveId, false);
    }
}
