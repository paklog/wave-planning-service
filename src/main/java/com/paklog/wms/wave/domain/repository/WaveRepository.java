package com.paklog.wms.wave.domain.repository;

import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.valueobject.WaveStatus;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Wave aggregate
 * Provides MongoDB persistence operations
 */
@Repository
public interface WaveRepository extends MongoRepository<Wave, String> {

    /**
     * Find waves by status
     */
    List<Wave> findByStatus(WaveStatus status);

    /**
     * Find waves by warehouse
     */
    List<Wave> findByWarehouseId(String warehouseId);

    /**
     * Find waves by warehouse and status
     */
    List<Wave> findByWarehouseIdAndStatus(String warehouseId, WaveStatus status);

    /**
     * Find waves ready to release
     * Waves must be in PLANNED status and planned release time has passed
     */
    @Query("{'status': ?0, 'plannedReleaseTime': {$lte: ?1}, 'inventoryAllocated': true}")
    List<Wave> findReadyToRelease(WaveStatus status, LocalDateTime time);

    /**
     * Find waves by zone
     */
    List<Wave> findByAssignedZone(String zone);

    /**
     * Find active waves (RELEASED or IN_PROGRESS)
     */
    @Query("{'status': {$in: ['RELEASED', 'IN_PROGRESS']}}")
    List<Wave> findActiveWaves();

    /**
     * Find waves containing specific order
     */
    @Query("{'orderIds': ?0}")
    Optional<Wave> findByOrderId(String orderId);

    /**
     * Get wave status distribution for warehouse
     */
    @Aggregation(pipeline = {
        "{ $match: { warehouseId: ?0 } }",
        "{ $group: { _id: '$status', count: { $sum: 1 } } }"
    })
    List<WaveStatusCount> getStatusDistribution(String warehouseId);

    /**
     * Get waves planned for a specific date range
     */
    @Query("{'plannedReleaseTime': {$gte: ?0, $lte: ?1}}")
    List<Wave> findByPlannedReleaseDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get waves by priority
     */
    List<Wave> findByPriority(com.paklog.wms.wave.domain.valueobject.WavePriority priority);

    /**
     * Count waves by status for a warehouse
     */
    long countByWarehouseIdAndStatus(String warehouseId, WaveStatus status);

    /**
     * Find waves created after a specific timestamp
     * Used for reconciliation
     */
    @Query("{'createdAt': {$gte: ?0}}")
    List<Wave> findWavesCreatedAfter(LocalDateTime since);

    /**
     * Result interface for aggregation queries
     */
    interface WaveStatusCount {
        WaveStatus getId();
        int getCount();
    }
}
