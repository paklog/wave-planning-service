package com.paklog.wms.wave.domain.service;

import com.paklog.wms.wave.infrastructure.client.OrderManagementClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain service for calculating SKU quantities for a wave.
 * Queries Order Management Service to get order line items and aggregate quantities.
 */
@Service
public class WaveSkuCalculationService {

    private static final Logger log = LoggerFactory.getLogger(WaveSkuCalculationService.class);

    private final OrderManagementClient orderManagementClient;

    public WaveSkuCalculationService(OrderManagementClient orderManagementClient) {
        this.orderManagementClient = orderManagementClient;
    }

    /**
     * Calculate total SKU quantities for all orders in a wave
     *
     * @param orderIds List of order IDs in the wave
     * @return Map of SKU to total quantity needed
     */
    public Map<String, Integer> calculateSkuQuantitiesForWave(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            log.warn("No order IDs provided for SKU calculation");
            return new HashMap<>();
        }

        log.info("Calculating SKU quantities for wave with {} orders", orderIds.size());

        try {
            Map<String, Integer> skuQuantities = orderManagementClient.calculateSkuQuantities(orderIds);

            log.info("SKU calculation complete: {} unique SKUs, {} total orders",
                    skuQuantities.size(), orderIds.size());

            // Log summary for monitoring
            int totalUnits = skuQuantities.values().stream().mapToInt(Integer::intValue).sum();
            log.debug("Wave requires {} total units across {} SKUs", totalUnits, skuQuantities.size());

            return skuQuantities;

        } catch (Exception e) {
            log.error("Error calculating SKU quantities for {} orders: {}",
                    orderIds.size(), e.getMessage(), e);
            // Return empty map as fallback - inventory allocation will work with orderIds
            return new HashMap<>();
        }
    }

    /**
     * Calculate SKU quantities for a single order (for validation/testing)
     */
    public Map<String, Integer> calculateSkuQuantitiesForOrder(String orderId) {
        OrderManagementClient.OrderDetails orderDetails = orderManagementClient.getOrderDetails(orderId);

        Map<String, Integer> skuQuantities = new HashMap<>();
        for (OrderManagementClient.OrderItem item : orderDetails.getItems()) {
            skuQuantities.put(item.getSku(), item.getQuantity());
        }

        return skuQuantities;
    }

    /**
     * Validate that order exists and has items
     */
    public boolean validateOrderHasItems(String orderId) {
        OrderManagementClient.OrderDetails orderDetails = orderManagementClient.getOrderDetails(orderId);
        return !orderDetails.isEmpty();
    }
}
