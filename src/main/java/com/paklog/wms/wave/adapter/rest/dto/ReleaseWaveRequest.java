package com.paklog.wms.wave.adapter.rest.dto;

/**
 * REST API request for releasing a wave
 */
public record ReleaseWaveRequest(
        boolean force
) {
    public ReleaseWaveRequest() {
        this(false);
    }
}
