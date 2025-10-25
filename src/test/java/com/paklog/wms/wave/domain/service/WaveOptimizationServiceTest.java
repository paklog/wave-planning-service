package com.paklog.wms.wave.domain.service;

import com.paklog.wms.wave.domain.aggregate.Wave;
import com.paklog.wms.wave.domain.entity.Order;
import com.paklog.wms.wave.domain.entity.Order.OrderLine;
import com.paklog.wms.wave.domain.entity.Order.ShippingMethod;
import com.paklog.wms.wave.domain.valueobject.CarrierCutoff;
import com.paklog.wms.wave.domain.valueobject.WaveCapacity;
import com.paklog.wms.wave.domain.valueobject.WavePriority;
import com.paklog.wms.wave.domain.valueobject.WaveStrategy;
import com.paklog.wms.wave.domain.valueobject.WaveStrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WaveOptimizationServiceTest {

    private WaveOptimizationService service;

    @BeforeEach
    void setUp() {
        service = new WaveOptimizationService();
    }

    @Test
    void optimizeWaveAppliesStrategiesAndUpdatesMetrics() {
        Wave wave = plannedWave("WAVE-OPT", List.of("O1", "O2", "O3", "O4", "O5"));
        List<Order> orders = List.of(
                order("O1", WavePriority.NORMAL, "FEDEX", "A1", LocalDateTime.now().plusHours(4), LocalDateTime.now().minusHours(2)),
                order("O2", WavePriority.HIGH, "FEDEX", "A2", LocalDateTime.now().plusHours(2), LocalDateTime.now().minusHours(2)),
                order("O3", WavePriority.CRITICAL, "UPS", "A1", LocalDateTime.now().plusHours(1), LocalDateTime.now().minusHours(1)),
                order("O4", WavePriority.LOW, "UPS", "A3", LocalDateTime.now().plusHours(6), LocalDateTime.now().minusHours(3)),
                order("O5", WavePriority.NORMAL, "DHL", "A2", LocalDateTime.now().plusHours(5), LocalDateTime.now().minusHours(4))
        );

        WaveOptimizationService.OptimizationCriteria criteria = WaveOptimizationService.OptimizationCriteria.defaultCriteria();
        service.optimizeWave(wave, orders, criteria);

        assertThat(wave.getOrderIds()).containsExactlyInAnyOrder("O1", "O2", "O3", "O4", "O5");
        assertThat(wave.getMetrics().getTotalOrders()).isEqualTo(5);

        criteria.setBalanceWorkload(false)
                .setMinimizeTravelDistance(false)
                .setPrioritizeSLA(false);
        assertThat(criteria.balanceWorkload()).isFalse();
        assertThat(criteria.minimizeTravelDistance()).isFalse();
        assertThat(criteria.prioritizeSLA()).isFalse();
    }

    @Test
    void createCarrierWavesGroupsOrdersByCarrierAndCutoff() {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            orders.add(order("CARRIER-" + i, WavePriority.NORMAL, "FEDEX", "Z1",
                    LocalDateTime.now().plusHours(4), LocalDateTime.now().minusMinutes(30)));
        }

        List<CarrierCutoff> cutoffs = List.of(new CarrierCutoff("FEDEX", LocalDateTime.now().plusHours(5), "EXPRESS"));

        List<Wave> waves = service.createCarrierWaves(orders, cutoffs);

        assertThat(waves).hasSize(1);
        assertThat(waves.getFirst().getOrderIds()).containsExactlyElementsOf(
                orders.stream().map(Order::getOrderId).toList());
    }

    @Test
    void createZoneWavesAssignsZones() {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            orders.add(order("ZONE-A-" + i, WavePriority.HIGH, "FEDEX", "ZONE-A",
                    LocalDateTime.now().plusHours(2), LocalDateTime.now()));
            orders.add(order("ZONE-B-" + i, WavePriority.NORMAL, "UPS", "ZONE-B",
                    LocalDateTime.now().plusHours(3), LocalDateTime.now()));
        }

        List<Wave> waves = service.createZoneWaves(orders, "WH-ZONE");

        assertThat(waves).hasSize(2);
        assertThat(waves.stream().map(Wave::getAssignedZone)).containsExactlyInAnyOrder("ZONE-A", "ZONE-B");
    }

    @Test
    void createCapacityBasedWavesSplitsOrdersWhenLimitsExceeded() {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            orders.add(order("CAP-" + i, WavePriority.NORMAL, "DHL", "ZONE-C",
                    LocalDateTime.now().plusHours(1), LocalDateTime.now()));
        }

        WaveCapacity capacity = WaveCapacity.builder()
                .maxOrders(5)
                .maxLines(500)
                .maxVolume(new BigDecimal("5000"))
                .maxWeight(new BigDecimal("5000"))
                .build();

        List<Wave> waves = service.createCapacityBasedWaves(orders, capacity, "WH-CAP");

        assertThat(waves).hasSizeGreaterThanOrEqualTo(2);
        assertThat(waves.getFirst().getOrderIds()).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void createTimeBasedWavesGroupsByWindow() {
        List<Order> orders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now().withMinute(10);
        for (int i = 0; i < 5; i++) {
            orders.add(order("TIME-" + i, WavePriority.NORMAL, "UPS", "ZONE-T",
                    now.plusHours(3), now.plusMinutes(i * 5L)));
        }

        List<Wave> waves = service.createTimeBasedWaves(orders, Duration.ofHours(1), "WH-TIME");

        assertThat(waves).hasSize(1);
        assertThat(waves.getFirst().getPlannedReleaseTime()).isNotNull();
    }

    private Wave plannedWave(String id, List<String> orderIds) {
        Wave wave = new Wave();
        wave.setWaveId(id);
        wave.plan(orderIds,
                WaveStrategy.builder()
                        .type(WaveStrategyType.TIME_BASED)
                        .timeInterval(Duration.ofMinutes(30))
                        .build(),
                "WH-1",
                WavePriority.NORMAL,
                LocalDateTime.now().plusHours(1));
        return wave;
    }

    private Order order(String id,
                        WavePriority priority,
                        String carrier,
                        String zone,
                        LocalDateTime requiredDate,
                        LocalDateTime orderDate) {
        return Order.builder()
                .orderId(id)
                .priority(priority)
                .shippingMethod(new ShippingMethod(carrier))
                .attributes(Map.of("primaryZone", zone))
                .orderLines(List.of(
                        new OrderLine(10),
                        new OrderLine(5)))
                .totalVolume(new BigDecimal("10"))
                .totalWeight(new BigDecimal("5"))
                .requiredDate(requiredDate)
                .orderDate(orderDate)
                .build();
    }
}

