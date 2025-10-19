package com.paklog.wms.wave.domain.service;

import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.entity.Order;
import com.paklog.wms.wave.domain.valueobject.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain service for wave optimization algorithms
 * Implements various strategies to optimize wave creation and release
 */
@Service
public class WaveOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(WaveOptimizationService.class);

    private static final int MAX_ORDERS_PER_WAVE = 100;
    private static final int MAX_LINES_PER_WAVE = 500;
    private static final BigDecimal MAX_VOLUME_PER_WAVE = new BigDecimal("1000.0");
    private static final int MIN_ORDERS_FOR_WAVE = 5;

    /**
     * Optimize wave using multi-objective optimization
     * Considers: travel distance, workload balance, SLA compliance
     */
    public Wave optimizeWave(Wave wave, List<Order> orders, OptimizationCriteria criteria) {
        logger.info("Optimizing wave {} with {} orders", wave.getWaveId(), orders.size());

        // Calculate current metrics
        WaveMetrics currentMetrics = calculateMetrics(orders);

        // Apply optimization strategies based on criteria
        List<Order> optimizedOrders = new ArrayList<>(orders);

        if (criteria.minimizeTravelDistance()) {
            optimizedOrders = optimizeForTravelDistance(optimizedOrders);
        }

        if (criteria.balanceWorkload()) {
            optimizedOrders = optimizeForWorkloadBalance(optimizedOrders);
        }

        if (criteria.prioritizeSLA()) {
            optimizedOrders = optimizeForSLA(optimizedOrders);
        }

        // Update wave with optimized order sequence
        wave.reorderOrders(optimizedOrders.stream()
                .map(Order::getOrderId)
                .collect(Collectors.toList()));

        WaveMetrics optimizedMetrics = calculateMetrics(optimizedOrders);
        logOptimizationResults(currentMetrics, optimizedMetrics);

        return wave;
    }

    /**
     * Create carrier-based waves
     * Groups orders by carrier and cutoff time
     */
    public List<Wave> createCarrierWaves(List<Order> orders, List<CarrierCutoff> cutoffs) {
        logger.info("Creating carrier-based waves for {} orders", orders.size());

        Map<String, List<Order>> ordersByCarrier = groupByCarrier(orders);
        List<Wave> waves = new ArrayList<>();

        for (Map.Entry<String, List<Order>> entry : ordersByCarrier.entrySet()) {
            String carrier = entry.getKey();
            List<Order> carrierOrders = entry.getValue();

            // Find cutoff for this carrier
            CarrierCutoff cutoff = cutoffs.stream()
                    .filter(c -> c.getCarrier().equals(carrier))
                    .findFirst()
                    .orElse(null);

            if (cutoff != null) {
                // Group by cutoff time windows
                List<Wave> carrierWaves = createWavesByTimeWindow(
                        carrierOrders,
                        cutoff.getCutoffTime(),
                        Duration.ofHours(2) // 2-hour windows before cutoff
                );
                waves.addAll(carrierWaves);
            }
        }

        logger.info("Created {} carrier-based waves", waves.size());
        return waves;
    }

    /**
     * Create zone-based waves
     * Groups orders by pick zone for efficiency
     */
    public List<Wave> createZoneWaves(List<Order> orders, String warehouseId) {
        logger.info("Creating zone-based waves for {} orders", orders.size());

        Map<String, List<Order>> ordersByZone = groupByZone(orders);
        List<Wave> waves = new ArrayList<>();

        for (Map.Entry<String, List<Order>> entry : ordersByZone.entrySet()) {
            String zone = entry.getKey();
            List<Order> zoneOrders = entry.getValue();

            // Calculate optimal wave size for this zone
            int optimalWaveSize = calculateOptimalWaveSize(zoneOrders, zone);

            // Create waves of optimal size
            List<List<Order>> batches = partitionOrders(zoneOrders, optimalWaveSize);

            for (List<Order> batch : batches) {
                Wave wave = createWaveFromOrders(batch, warehouseId, WaveStrategyType.ZONE_BASED);
                wave.assignZone(zone);
                waves.add(wave);
            }
        }

        logger.info("Created {} zone-based waves", waves.size());
        return waves;
    }

    /**
     * Create capacity-based waves
     * Ensures waves don't exceed resource constraints
     */
    public List<Wave> createCapacityBasedWaves(
            List<Order> orders,
            WaveCapacity capacity,
            String warehouseId) {

        logger.info("Creating capacity-based waves for {} orders", orders.size());

        List<Wave> waves = new ArrayList<>();
        List<Order> currentBatch = new ArrayList<>();
        WaveMetrics currentMetrics = new WaveMetrics();

        // Sort orders by priority first
        List<Order> sortedOrders = orders.stream()
                .sorted(Comparator.comparing(Order::getPriority).reversed()
                        .thenComparing(Order::getRequiredDate))
                .collect(Collectors.toList());

        for (Order order : sortedOrders) {
            // Calculate what metrics would be if we add this order
            WaveMetrics testMetrics = calculateMetrics(
                    combine(currentBatch, order));

            // Check if adding this order exceeds capacity
            if (wouldExceedCapacity(testMetrics, capacity)) {
                // Create wave from current batch
                if (!currentBatch.isEmpty() && currentBatch.size() >= MIN_ORDERS_FOR_WAVE) {
                    Wave wave = createWaveFromOrders(
                            currentBatch,
                            warehouseId,
                            WaveStrategyType.CAPACITY_BASED);
                    waves.add(wave);
                }

                // Start new batch
                currentBatch = new ArrayList<>();
                currentBatch.add(order);
                currentMetrics = calculateMetrics(currentBatch);
            } else {
                currentBatch.add(order);
                currentMetrics = testMetrics;
            }
        }

        // Add remaining orders as final wave
        if (!currentBatch.isEmpty() && currentBatch.size() >= MIN_ORDERS_FOR_WAVE) {
            Wave wave = createWaveFromOrders(
                    currentBatch,
                    warehouseId,
                    WaveStrategyType.CAPACITY_BASED);
            waves.add(wave);
        }

        logger.info("Created {} capacity-based waves", waves.size());
        return waves;
    }

    /**
     * Create time-based waves
     * Groups orders into time windows
     */
    public List<Wave> createTimeBasedWaves(
            List<Order> orders,
            Duration windowSize,
            String warehouseId) {

        logger.info("Creating time-based waves with {} window", windowSize);

        Map<LocalDateTime, List<Order>> ordersByWindow =
                groupByTimeWindow(orders, windowSize);

        List<Wave> waves = new ArrayList<>();

        for (Map.Entry<LocalDateTime, List<Order>> entry : ordersByWindow.entrySet()) {
            LocalDateTime windowStart = entry.getKey();
            List<Order> windowOrders = entry.getValue();

            if (windowOrders.size() >= MIN_ORDERS_FOR_WAVE) {
                Wave wave = createWaveFromOrders(
                        windowOrders,
                        warehouseId,
                        WaveStrategyType.TIME_BASED);

                wave.setPlannedReleaseTime(windowStart);
                waves.add(wave);
            }
        }

        logger.info("Created {} time-based waves", waves.size());
        return waves;
    }

    /**
     * Optimize order sequence to minimize travel distance
     * Uses a greedy nearest-neighbor approach
     */
    private List<Order> optimizeForTravelDistance(List<Order> orders) {
        if (orders.size() <= 1) {
            return orders;
        }

        List<Order> optimized = new ArrayList<>();
        Set<Order> unvisited = new HashSet<>(orders);

        // Start with highest priority order
        Order current = orders.stream()
                .max(Comparator.comparing(Order::getPriority))
                .orElse(orders.get(0));

        optimized.add(current);
        unvisited.remove(current);

        // Greedy nearest neighbor
        while (!unvisited.isEmpty()) {
            final Order currentOrder = current;

            Order nearest = unvisited.stream()
                    .min(Comparator.comparing(o ->
                            estimateDistance(currentOrder, o)))
                    .orElse(null);

            if (nearest != null) {
                optimized.add(nearest);
                unvisited.remove(nearest);
                current = nearest;
            } else {
                break;
            }
        }

        // Add any remaining orders
        optimized.addAll(unvisited);

        logger.debug("Optimized {} orders for travel distance", orders.size());
        return optimized;
    }

    /**
     * Optimize for workload balance
     * Tries to create similar sized groups
     */
    private List<Order> optimizeForWorkloadBalance(List<Order> orders) {
        // Sort by number of lines to balance picking workload
        return orders.stream()
                .sorted(Comparator.comparing(o -> o.getOrderLines().size()))
                .collect(Collectors.toList());
    }

    /**
     * Optimize for SLA compliance
     * Prioritizes orders with tighter deadlines
     */
    private List<Order> optimizeForSLA(List<Order> orders) {
        LocalDateTime now = LocalDateTime.now();

        return orders.stream()
                .sorted(Comparator
                        .comparing((Order o) -> Duration.between(now, o.getRequiredDate()))
                        .thenComparing(Order::getPriority).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Calculate wave metrics
     */
    private WaveMetrics calculateMetrics(List<Order> orders) {
        WaveMetrics metrics = new WaveMetrics();

        int totalOrders = orders.size();
        int totalLines = orders.stream()
                .mapToInt(o -> o.getOrderLines().size())
                .sum();

        int totalUnits = orders.stream()
                .flatMap(o -> o.getOrderLines().stream())
                .mapToInt(line -> line.getQuantity())
                .sum();

        BigDecimal totalVolume = orders.stream()
                .map(Order::calculateVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWeight = orders.stream()
                .map(Order::calculateWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        metrics.setTotalOrders(totalOrders);
        metrics.setTotalLines(totalLines);
        metrics.setTotalUnits(totalUnits);
        metrics.setTotalVolume(totalVolume);
        metrics.setTotalWeight(totalWeight);

        // Estimate pickers needed (1 picker per 50 lines)
        int estimatedPickers = Math.max(1, (totalLines / 50) + 1);
        metrics.setEstimatedPickers(estimatedPickers);

        // Estimate completion time (10 lines per hour per picker)
        int hoursNeeded = totalLines / (10 * estimatedPickers);
        metrics.setEstimatedCompletionTime(
                LocalDateTime.now().plusHours(hoursNeeded));

        return metrics;
    }

    /**
     * Group orders by carrier
     */
    private Map<String, List<Order>> groupByCarrier(List<Order> orders) {
        return orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getShippingMethod().getCarrier(),
                        Collectors.toList()));
    }

    /**
     * Group orders by zone
     */
    private Map<String, List<Order>> groupByZone(List<Order> orders) {
        return orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getAttribute("primaryZone", "DEFAULT"),
                        Collectors.toList()));
    }

    /**
     * Group orders by time window
     */
    private Map<LocalDateTime, List<Order>> groupByTimeWindow(
            List<Order> orders,
            Duration windowSize) {

        return orders.stream()
                .collect(Collectors.groupingBy(
                        o -> roundToWindow(o.getOrderDate(), windowSize),
                        Collectors.toList()));
    }

    /**
     * Round time to window boundary
     */
    private LocalDateTime roundToWindow(LocalDateTime time, Duration windowSize) {
        long hours = windowSize.toHours();
        return time.withMinute(0).withSecond(0).withNano(0)
                .withHour((int) (time.getHour() / hours * hours));
    }

    /**
     * Calculate optimal wave size for a zone
     */
    private int calculateOptimalWaveSize(List<Order> orders, String zone) {
        // Base optimal size on zone characteristics
        // For now, use average but can be enhanced with zone-specific logic
        int totalLines = orders.stream()
                .mapToInt(o -> o.getOrderLines().size())
                .sum();

        int avgLinesPerOrder = totalLines / Math.max(1, orders.size());

        // Target 200-300 lines per wave for efficiency
        return Math.max(MIN_ORDERS_FOR_WAVE,
                Math.min(MAX_ORDERS_PER_WAVE, 250 / Math.max(1, avgLinesPerOrder)));
    }

    /**
     * Partition orders into batches
     */
    private List<List<Order>> partitionOrders(List<Order> orders, int batchSize) {
        List<List<Order>> batches = new ArrayList<>();

        for (int i = 0; i < orders.size(); i += batchSize) {
            int end = Math.min(i + batchSize, orders.size());
            batches.add(new ArrayList<>(orders.subList(i, end)));
        }

        return batches;
    }

    /**
     * Create waves by time window before cutoff
     */
    private List<Wave> createWavesByTimeWindow(
            List<Order> orders,
            LocalDateTime cutoff,
            Duration windowSize) {

        List<Wave> waves = new ArrayList<>();
        Map<LocalDateTime, List<Order>> ordersByWindow =
                groupByTimeWindow(orders, windowSize);

        for (Map.Entry<LocalDateTime, List<Order>> entry : ordersByWindow.entrySet()) {
            LocalDateTime windowStart = entry.getKey();

            // Only create wave if window is before cutoff
            if (windowStart.isBefore(cutoff)) {
                List<Order> windowOrders = entry.getValue();
                if (windowOrders.size() >= MIN_ORDERS_FOR_WAVE) {
                    Wave wave = createWaveFromOrders(
                            windowOrders,
                            "WAREHOUSE",
                            WaveStrategyType.CARRIER_BASED);
                    wave.setPlannedReleaseTime(windowStart);
                    waves.add(wave);
                }
            }
        }

        return waves;
    }

    /**
     * Create wave from list of orders
     */
    private Wave createWaveFromOrders(
            List<Order> orders,
            String warehouseId,
            WaveStrategyType strategyType) {

        Wave wave = new Wave();
        WaveStrategy strategy = WaveStrategy.builder()
                .type(strategyType)
                .build();

        wave.plan(
                orders.stream().map(Order::getOrderId).collect(Collectors.toList()),
                strategy,
                warehouseId,
                1,
                LocalDateTime.now().plusHours(1)
        );

        return wave;
    }

    /**
     * Check if adding orders would exceed capacity
     */
    private boolean wouldExceedCapacity(WaveMetrics metrics, WaveCapacity capacity) {
        return metrics.getTotalOrders() > capacity.getMaxOrders()
                || metrics.getTotalLines() > capacity.getMaxLines()
                || metrics.getTotalVolume().compareTo(capacity.getMaxVolume()) > 0
                || metrics.getTotalWeight().compareTo(capacity.getMaxWeight()) > 0;
    }

    /**
     * Combine list with single order
     */
    private List<Order> combine(List<Order> orders, Order newOrder) {
        List<Order> combined = new ArrayList<>(orders);
        combined.add(newOrder);
        return combined;
    }

    /**
     * Estimate distance between two orders based on zones
     * Simple heuristic - can be enhanced with actual location data
     */
    private double estimateDistance(Order o1, Order o2) {
        String zone1 = o1.getAttribute("primaryZone", "DEFAULT");
        String zone2 = o2.getAttribute("primaryZone", "DEFAULT");

        // Same zone = close distance
        if (zone1.equals(zone2)) {
            return 1.0;
        }

        // Different zones = higher distance
        return 10.0;
    }

    /**
     * Log optimization results
     */
    private void logOptimizationResults(
            WaveMetrics before,
            WaveMetrics after) {

        logger.info("Optimization Results:");
        logger.info("  Orders: {} (unchanged)", after.getTotalOrders());
        logger.info("  Lines: {} (unchanged)", after.getTotalLines());
        logger.info("  Estimated completion: {} -> {}",
                before.getEstimatedCompletionTime(),
                after.getEstimatedCompletionTime());
    }

    /**
     * Optimization criteria configuration
     */
    public static class OptimizationCriteria {
        private boolean minimizeTravelDistance = true;
        private boolean balanceWorkload = true;
        private boolean prioritizeSLA = true;

        public static OptimizationCriteria defaultCriteria() {
            return new OptimizationCriteria();
        }

        public boolean minimizeTravelDistance() {
            return minimizeTravelDistance;
        }

        public boolean balanceWorkload() {
            return balanceWorkload;
        }

        public boolean prioritizeSLA() {
            return prioritizeSLA;
        }

        public OptimizationCriteria setMinimizeTravelDistance(boolean value) {
            this.minimizeTravelDistance = value;
            return this;
        }

        public OptimizationCriteria setBalanceWorkload(boolean value) {
            this.balanceWorkload = value;
            return this;
        }

        public OptimizationCriteria setPrioritizeSLA(boolean value) {
            this.prioritizeSLA = value;
            return this;
        }
    }
}
