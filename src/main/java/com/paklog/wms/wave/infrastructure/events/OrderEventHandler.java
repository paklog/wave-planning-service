package com.paklog.wms.wave.infrastructure.events;

import com.paklog.wms.wave.application.service.WavePlanningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event handler for Order and Inventory events
 * Listens to events that affect wave planning
 */
@Component
public class OrderEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventHandler.class);

    private final WavePlanningService wavePlanningService;

    public OrderEventHandler(WavePlanningService wavePlanningService) {
        this.wavePlanningService = wavePlanningService;
    }

    /**
     * Handle FulfillmentOrderValidatedEvent from order-management-service
     * Adds eligible orders to the wave planning queue
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.order-events:wms-order-events}",
            groupId = "${paklog.kafka.consumer.group-id:wave-planning-service}"
    )
    public void handleOrderValidated(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"FulfillmentOrderValidatedEvent".equals(eventType)) {
                return; // Ignore other event types
            }

            logger.info("Received FulfillmentOrderValidatedEvent: {}", eventData);

            String orderId = (String) eventData.get("orderId");
            String warehouseId = (String) eventData.get("warehouseId");
            String priority = (String) eventData.get("priority");
            String shippingMethod = (String) eventData.get("shippingMethod");

            // Add order to wave planning queue
            // In a real system, this would check eligibility criteria
            logger.info("Order {} validated for warehouse {}, ready for wave planning",
                    orderId, warehouseId);

            // This could trigger automatic wave planning based on:
            // - Time-based batching (every 30 minutes)
            // - Quantity threshold (when 50 orders are ready)
            // - Carrier cutoff times
            // - Priority levels

        } catch (Exception e) {
            logger.error("Error handling FulfillmentOrderValidatedEvent", e);
            // In production, publish to dead letter queue
        }
    }

    /**
     * Handle InventoryAllocatedEvent from inventory-service
     * Marks wave as ready for release when inventory is allocated
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.inventory-events:wms-inventory-events}",
            groupId = "${paklog.kafka.consumer.group-id:wave-planning-service}"
    )
    public void handleInventoryAllocated(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"InventoryAllocatedEvent".equals(eventType)) {
                return;
            }

            logger.info("Received InventoryAllocatedEvent: {}", eventData);

            String orderId = (String) eventData.get("orderId");
            String warehouseId = (String) eventData.get("warehouseId");
            Boolean fullyAllocated = (Boolean) eventData.getOrDefault("fullyAllocated", false);

            if (fullyAllocated) {
                // Find wave containing this order and mark as ready if all orders allocated
                logger.info("Order {} fully allocated in warehouse {}, wave may be ready for release",
                        orderId, warehouseId);

                // This would trigger:
                // 1. Check if all orders in the wave are allocated
                // 2. If yes, mark wave as ready for release
                // 3. Optionally auto-release based on configuration
            } else {
                logger.warn("Order {} partially allocated - wave release may be delayed", orderId);
            }

        } catch (Exception e) {
            logger.error("Error handling InventoryAllocatedEvent", e);
        }
    }

    /**
     * Handle OrderCancelledEvent from order-management-service
     * Removes orders from waves if cancelled before release
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.order-events:wms-order-events}",
            groupId = "${paklog.kafka.consumer.group-id:wave-planning-service}"
    )
    public void handleOrderCancelled(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"OrderCancelledEvent".equals(eventType)) {
                return;
            }

            logger.warn("Received OrderCancelledEvent: {}", eventData);

            String orderId = (String) eventData.get("orderId");
            String reason = (String) eventData.get("reason");

            logger.warn("Order {} cancelled: {}", orderId, reason);

            // This would:
            // 1. Find wave containing this order
            // 2. If wave status is PLANNED (not released), remove order
            // 3. If wave is already released, handle cancellation differently
            //    (may need to cancel pick tasks)

        } catch (Exception e) {
            logger.error("Error handling OrderCancelledEvent", e);
        }
    }

    /**
     * Handle InventoryShortageEvent from inventory-service
     * Handles situations where inventory becomes unavailable after wave planning
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.inventory-events:wms-inventory-events}",
            groupId = "${paklog.kafka.consumer.group-id:wave-planning-service}"
    )
    public void handleInventoryShortage(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"InventoryShortageEvent".equals(eventType)) {
                return;
            }

            logger.warn("Received InventoryShortageEvent: {}", eventData);

            String orderId = (String) eventData.get("orderId");
            String sku = (String) eventData.get("sku");
            Integer requiredQty = ((Number) eventData.get("requiredQuantity")).intValue();
            Integer availableQty = ((Number) eventData.get("availableQuantity")).intValue();

            logger.warn("Inventory shortage for order {}: SKU {} needs {} but only {} available",
                    orderId, sku, requiredQty, availableQty);

            // This would:
            // 1. Find wave containing this order
            // 2. If not released, may delay wave release
            // 3. If released, may need to handle as pick shortage
            // 4. Trigger alerts to warehouse management

        } catch (Exception e) {
            logger.error("Error handling InventoryShortageEvent", e);
        

}
}
}
