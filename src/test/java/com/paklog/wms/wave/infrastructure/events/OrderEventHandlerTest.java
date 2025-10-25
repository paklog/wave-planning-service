package com.paklog.wms.wave.infrastructure.events;

import com.paklog.wms.wave.application.service.WavePlanningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderEventHandlerTest {

    @Mock
    private WavePlanningService wavePlanningService;

    @Test
    void handleOrderValidatedProcessesMatchingEvents() {
        OrderEventHandler handler = new OrderEventHandler(wavePlanningService);

        handler.handleOrderValidated(Map.of(
                "type", "FulfillmentOrderValidatedEvent",
                "orderId", "ORD-1",
                "warehouseId", "WH-1",
                "priority", "HIGH",
                "shippingMethod", "EXPRESS"
        ));

        verifyNoInteractions(wavePlanningService);
    }

    @Test
    void handleOrderValidatedIgnoresOtherEvents() {
        OrderEventHandler handler = new OrderEventHandler(wavePlanningService);

        handler.handleOrderValidated(Map.of(
                "type", "OtherEvent",
                "orderId", "ORD-2"
        ));

        verifyNoInteractions(wavePlanningService);
    }

    @Test
    void handleInventoryAllocatedCoversBothBranches() {
        OrderEventHandler handler = new OrderEventHandler(wavePlanningService);

        handler.handleInventoryAllocated(Map.of(
                "type", "InventoryAllocatedEvent",
                "orderId", "ORD-3",
                "warehouseId", "WH-2",
                "fullyAllocated", true
        ));

        handler.handleInventoryAllocated(Map.of(
                "type", "InventoryAllocatedEvent",
                "orderId", "ORD-4",
                "warehouseId", "WH-2",
                "fullyAllocated", false
        ));

        verifyNoInteractions(wavePlanningService);
    }

    @Test
    void handleOrderCancelledLogsEvent() {
        OrderEventHandler handler = new OrderEventHandler(wavePlanningService);

        handler.handleOrderCancelled(Map.of(
                "type", "OrderCancelledEvent",
                "orderId", "ORD-5",
                "reason", "Customer request"
        ));

        verifyNoInteractions(wavePlanningService);
    }

    @Test
    void handleInventoryShortageLogsWarning() {
        OrderEventHandler handler = new OrderEventHandler(wavePlanningService);

        handler.handleInventoryShortage(Map.of(
                "type", "InventoryShortageEvent",
                "orderId", "ORD-6",
                "sku", "SKU-1",
                "requiredQuantity", 10,
                "availableQuantity", 2
        ));

        verifyNoInteractions(wavePlanningService);
    }
}
