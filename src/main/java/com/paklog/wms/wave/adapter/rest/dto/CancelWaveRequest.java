package com.paklog.wms.wave.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * REST API request for cancelling a wave
 */
public record CancelWaveRequest(
        @NotBlank(message = "Cancellation reason is required")
        String reason
) {
}
