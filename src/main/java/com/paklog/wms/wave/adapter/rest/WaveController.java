package com.paklog.wms.wave.adapter.rest;

import com.paklog.wms.wave.adapter.rest.dto.*;
import com.paklog.wms.wave.application.command.AssignZoneCommand;
import com.paklog.wms.wave.application.command.CancelWaveCommand;
import com.paklog.wms.wave.application.command.CreateWaveCommand;
import com.paklog.wms.wave.application.command.ReleaseWaveCommand;
import com.paklog.wms.wave.application.service.WavePlanningService;
import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.valueobject.WaveStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for wave management
 */
@RestController
@RequestMapping("/api/v1/waves")
@Tag(name = "Wave Management", description = "Wave planning and management operations")
public class WaveController {

    private final WavePlanningService wavePlanningService;

    public WaveController(WavePlanningService wavePlanningService) {
        this.wavePlanningService = wavePlanningService;
    }

    @PostMapping
    @Operation(summary = "Create new wave", description = "Create and plan a new wave with orders")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Wave created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<WaveResponse> createWave(@Valid @RequestBody CreateWaveRequest request) {
        CreateWaveCommand command = new CreateWaveCommand(
                request.orderIds(),
                request.strategy(),
                request.warehouseId(),
                request.priority(),
                request.plannedReleaseTime(),
                request.maxOrders(),
                request.maxLines(),
                request.timeInterval() != null ? Duration.parse(request.timeInterval()) : null
        );

        Wave wave = wavePlanningService.createWave(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WaveResponse.fromDomain(wave));
    }

    @GetMapping
    @Operation(summary = "List waves", description = "List waves with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Waves retrieved successfully")
    })
    public ResponseEntity<List<WaveResponse>> listWaves(
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) WaveStatus status,

            @Parameter(description = "Filter by warehouse ID")
            @RequestParam(required = false) String warehouseId
    ) {
        List<Wave> waves;

        if (warehouseId != null && status != null) {
            waves = wavePlanningService.findWavesByWarehouseAndStatus(warehouseId, status);
        } else if (status == null) {
            waves = wavePlanningService.findActiveWaves();
        } else {
            waves = List.of();
        }

        List<WaveResponse> response = waves.stream()
                .map(WaveResponse::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{waveId}")
    @Operation(summary = "Get wave details", description = "Get detailed information about a wave")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wave found"),
            @ApiResponse(responseCode = "404", description = "Wave not found")
    })
    public ResponseEntity<WaveResponse> getWave(
            @Parameter(description = "Wave ID", required = true)
            @PathVariable String waveId
    ) {
        Wave wave = wavePlanningService.findWaveById(waveId);
        return ResponseEntity.ok(WaveResponse.fromDomain(wave));
    }

    @PostMapping("/{waveId}/release")
    @Operation(summary = "Release wave", description = "Release wave for execution")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wave released successfully"),
            @ApiResponse(responseCode = "404", description = "Wave not found"),
            @ApiResponse(responseCode = "409", description = "Wave cannot be released")
    })
    public ResponseEntity<WaveResponse> releaseWave(
            @Parameter(description = "Wave ID", required = true)
            @PathVariable String waveId,

            @RequestBody(required = false) ReleaseWaveRequest request
    ) {
        ReleaseWaveCommand command = new ReleaseWaveCommand(
                waveId,
                request != null && request.force()
        );

        Wave wave = wavePlanningService.releaseWave(command);
        return ResponseEntity.ok(WaveResponse.fromDomain(wave));
    }

    @PostMapping("/{waveId}/cancel")
    @Operation(summary = "Cancel wave", description = "Cancel a wave with a reason")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wave cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Wave not found"),
            @ApiResponse(responseCode = "409", description = "Wave cannot be cancelled")
    })
    public ResponseEntity<WaveResponse> cancelWave(
            @Parameter(description = "Wave ID", required = true)
            @PathVariable String waveId,

            @Valid @RequestBody CancelWaveRequest request
    ) {
        CancelWaveCommand command = new CancelWaveCommand(waveId, request.reason());

        Wave wave = wavePlanningService.cancelWave(command);
        return ResponseEntity.ok(WaveResponse.fromDomain(wave));
    }

    @PostMapping("/{waveId}/assign-zone")
    @Operation(summary = "Assign zone to wave", description = "Assign a warehouse zone to a wave")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zone assigned successfully"),
            @ApiResponse(responseCode = "404", description = "Wave not found"),
            @ApiResponse(responseCode = "409", description = "Zone cannot be assigned")
    })
    public ResponseEntity<WaveResponse> assignZone(
            @Parameter(description = "Wave ID", required = true)
            @PathVariable String waveId,

            @Parameter(description = "Zone ID", required = true)
            @RequestParam String zone
    ) {
        AssignZoneCommand command = new AssignZoneCommand(waveId, zone);

        Wave wave = wavePlanningService.assignZone(command);
        return ResponseEntity.ok(WaveResponse.fromDomain(wave));
    }

    @GetMapping("/ready-to-release")
    @Operation(summary = "Get waves ready to release", description = "Get all waves that are ready to be released")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Waves retrieved successfully")
    })
    public ResponseEntity<List<WaveResponse>> getWavesReadyToRelease() {
        List<Wave> waves = wavePlanningService.findWavesReadyToRelease();
        List<WaveResponse> response = waves.stream()
                .map(WaveResponse::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
