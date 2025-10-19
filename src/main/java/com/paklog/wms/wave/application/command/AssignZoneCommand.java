package com.paklog.wms.wave.application.command;

/**
 * Command to assign a zone to a wave
 */
public record AssignZoneCommand(
        String waveId,
        String zone
) {
    public AssignZoneCommand {
        if (waveId == null || waveId.isBlank()) {
            throw new IllegalArgumentException("Wave ID cannot be null or empty");
        }
        if (zone == null || zone.isBlank()) {
            throw new IllegalArgumentException("Zone cannot be null or empty");
        }
    }
}
