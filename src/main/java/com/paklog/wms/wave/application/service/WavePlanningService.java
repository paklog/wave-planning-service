package com.paklog.wms.wave.application.service;

import com.paklog.wms.wave.application.command.AssignZoneCommand;
import com.paklog.wms.wave.application.command.CancelWaveCommand;
import com.paklog.wms.wave.application.command.CreateWaveCommand;
import com.paklog.wms.wave.application.command.ReleaseWaveCommand;
import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.repository.WaveRepository;
import com.paklog.wms.wave.domain.valueobject.WaveId;
import com.paklog.wms.wave.domain.valueobject.WaveStatus;
import com.paklog.wms.wave.domain.valueobject.WaveStrategy;
import com.paklog.wms.wave.infrastructure.events.WaveEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Application service for wave planning operations
 * Orchestrates wave creation, release, and lifecycle management
 */
@Service
public class WavePlanningService {

    private static final Logger logger = LoggerFactory.getLogger(WavePlanningService.class);

    private final WaveRepository waveRepository;
    private final WaveEventPublisher eventPublisher;

    public WavePlanningService(WaveRepository waveRepository, WaveEventPublisher eventPublisher) {
        this.waveRepository = waveRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create and plan a new wave
     */
    @Transactional
    public Wave createWave(CreateWaveCommand command) {
        logger.info("Creating wave for warehouse: {}, orders: {}",
                command.warehouseId(), command.orderIds().size());

        // Generate wave ID
        String waveId = WaveId.generate().getValue();

        // Build strategy from command
        WaveStrategy strategy = buildStrategy(command);

        // Create wave
        Wave wave = new Wave();
        wave.setWaveId(waveId);
        wave.plan(
                command.orderIds(),
                strategy,
                command.warehouseId(),
                command.priority(),
                command.plannedReleaseTime()
        );

        // Save wave
        Wave savedWave = waveRepository.save(wave);

        // Publish domain events
        eventPublisher.publishEvents(wave.getDomainEvents());
        wave.clearDomainEvents();

        logger.info("Wave created successfully: {}", waveId);
        return savedWave;
    }

    /**
     * Release a wave for execution
     */
    @Transactional
    public Wave releaseWave(ReleaseWaveCommand command) {
        logger.info("Releasing wave: {}", command.waveId());

        Wave wave = waveRepository.findById(command.waveId())
                .orElseThrow(() -> new WaveNotFoundException(command.waveId()));

        // Release the wave
        wave.release();

        // Save updated wave
        Wave savedWave = waveRepository.save(wave);

        // Publish domain events
        eventPublisher.publishEvents(wave.getDomainEvents());
        wave.clearDomainEvents();

        logger.info("Wave released successfully: {}", command.waveId());
        return savedWave;
    }

    /**
     * Cancel a wave
     */
    @Transactional
    public Wave cancelWave(CancelWaveCommand command) {
        logger.info("Cancelling wave: {}, reason: {}", command.waveId(), command.reason());

        Wave wave = waveRepository.findById(command.waveId())
                .orElseThrow(() -> new WaveNotFoundException(command.waveId()));

        // Cancel the wave
        wave.cancel(command.reason());

        // Save updated wave
        Wave savedWave = waveRepository.save(wave);

        // Publish domain events
        eventPublisher.publishEvents(wave.getDomainEvents());
        wave.clearDomainEvents();

        logger.info("Wave cancelled successfully: {}", command.waveId());
        return savedWave;
    }

    /**
     * Assign zone to a wave
     */
    @Transactional
    public Wave assignZone(AssignZoneCommand command) {
        logger.info("Assigning zone {} to wave: {}", command.zone(), command.waveId());

        Wave wave = waveRepository.findById(command.waveId())
                .orElseThrow(() -> new WaveNotFoundException(command.waveId()));

        wave.assignZone(command.zone());

        Wave savedWave = waveRepository.save(wave);

        logger.info("Zone assigned successfully to wave: {}", command.waveId());
        return savedWave;
    }

    /**
     * Find waves ready to release
     */
    public List<Wave> findWavesReadyToRelease() {
        return waveRepository.findReadyToRelease(WaveStatus.PLANNED, LocalDateTime.now());
    }

    /**
     * Find wave by ID
     */
    public Wave findWaveById(String waveId) {
        return waveRepository.findById(waveId)
                .orElseThrow(() -> new WaveNotFoundException(waveId));
    }

    /**
     * Find waves by warehouse and status
     */
    public List<Wave> findWavesByWarehouseAndStatus(String warehouseId, WaveStatus status) {
        return waveRepository.findByWarehouseIdAndStatus(warehouseId, status);
    }

    /**
     * Find all active waves
     */
    public List<Wave> findActiveWaves() {
        return waveRepository.findActiveWaves();
    }

    private WaveStrategy buildStrategy(CreateWaveCommand command) {
        WaveStrategy.Builder builder = WaveStrategy.builder()
                .type(command.strategyType());

        if (command.maxOrders() != null) {
            builder.maxOrders(command.maxOrders());
        }
        if (command.maxLines() != null) {
            builder.maxLines(command.maxLines());
        }
        if (command.timeInterval() != null) {
            builder.timeInterval(command.timeInterval());
        }

        return builder.build();
    }

    /**
     * Exception thrown when wave is not found
     */
    public static class WaveNotFoundException extends RuntimeException {
        public WaveNotFoundException(String waveId) {
            super("Wave not found: " + waveId);
        

}
}
}
