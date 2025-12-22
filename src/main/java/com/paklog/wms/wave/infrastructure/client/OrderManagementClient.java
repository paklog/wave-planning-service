package com.paklog.wms.wave.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Client for querying Order Management Service to get order details.
 * Used by Wave Planning to calculate SKU quantities for inventory allocation.
 *
 * Resilience Features:
 * - Circuit Breaker: Opens after 50% failure rate (min 10 requests)
 * - Retry: 3 attempts with exponential backoff (1s, 2s, 4s)
 * - Fallback: Returns empty order details on failure
 */
@Component
public class OrderManagementClient {

    private static final Logger log = LoggerFactory.getLogger(OrderManagementClient.class);
    private static final String CIRCUIT_BREAKER_NAME = "orderManagementService";
    private static final String RETRY_NAME = "orderManagementService";

    private final RestTemplate restTemplate;
    private final String orderManagementBaseUrl;

    public OrderManagementClient(RestTemplate restTemplate,
                                @Value("${wave-planning.order-management.url:http://order-management:8080}")
                                String orderManagementBaseUrl) {
        this.restTemplate = restTemplate;
        this.orderManagementBaseUrl = orderManagementBaseUrl;
    }

    /**
     * Get order details for a single order
     *
     * Circuit Breaker: Falls back to empty order details when service is unavailable
     * Retry: 3 attempts with exponential backoff before giving up
     */
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getOrderDetailsFallback")
    @Retry(name = RETRY_NAME)
    public OrderDetails getOrderDetails(String orderId) {
        String url = orderManagementBaseUrl + "/api/v1/fulfillment-orders/" + orderId;
        log.debug("Fetching order details from: {}", url);

        ResponseEntity<OrderDetailsResponse> response = restTemplate.getForEntity(
                url,
                OrderDetailsResponse.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return mapToOrderDetails(response.getBody());
        }

        log.warn("Failed to fetch order details for orderId: {}, status: {}",
                orderId, response.getStatusCode());
        throw new OrderManagementServiceException(
                "Failed to fetch order details: " + response.getStatusCode());
    }

    /**
     * Fallback method for circuit breaker
     * Returns empty order details when Order Management Service is unavailable
     */
    private OrderDetails getOrderDetailsFallback(String orderId, Exception e) {
        log.error("Circuit breaker activated for orderId: {}, returning empty order. Error: {}",
                orderId, e.getMessage());
        return OrderDetails.empty(orderId);
    }

    /**
     * Get order details for multiple orders (batch)
     */
    public List<OrderDetails> getOrderDetails(List<String> orderIds) {
        return orderIds.stream()
                .map(this::getOrderDetails)
                .collect(Collectors.toList());
    }

    /**
     * Calculate total SKU quantities across multiple orders
     */
    public Map<String, Integer> calculateSkuQuantities(List<String> orderIds) {
        log.info("Calculating SKU quantities for {} orders", orderIds.size());

        Map<String, Integer> skuQuantities = new HashMap<>();

        for (String orderId : orderIds) {
            OrderDetails orderDetails = getOrderDetails(orderId);

            for (OrderItem item : orderDetails.getItems()) {
                skuQuantities.merge(item.getSku(), item.getQuantity(), Integer::sum);
            }
        }

        log.info("Calculated SKU quantities: {} unique SKUs across {} orders",
                skuQuantities.size(), orderIds.size());

        return skuQuantities;
    }

    // Helper method to map REST response to domain model
    private OrderDetails mapToOrderDetails(OrderDetailsResponse response) {
        List<OrderItem> items = response.getItems() != null
                ? response.getItems().stream()
                .map(i -> new OrderItem(i.getSellerSku(), i.getQuantity()))
                .collect(Collectors.toList())
                : Collections.emptyList();

        return new OrderDetails(
                response.getOrderId(),
                response.getSellerFulfillmentOrderId(),
                items
        );
    }

    // DTO classes for REST API
    public static class OrderDetailsResponse {
        private String orderId;
        private String sellerFulfillmentOrderId;
        private List<OrderItemResponse> items;

        // Getters and setters
        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getSellerFulfillmentOrderId() {
            return sellerFulfillmentOrderId;
        }

        public void setSellerFulfillmentOrderId(String sellerFulfillmentOrderId) {
            this.sellerFulfillmentOrderId = sellerFulfillmentOrderId;
        }

        public List<OrderItemResponse> getItems() {
            return items;
        }

        public void setItems(List<OrderItemResponse> items) {
            this.items = items;
        }
    }

    public static class OrderItemResponse {
        private String sellerSku;
        private Integer quantity;

        public String getSellerSku() {
            return sellerSku;
        }

        public void setSellerSku(String sellerSku) {
            this.sellerSku = sellerSku;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    // Domain model classes
    public static class OrderDetails {
        private final String orderId;
        private final String sellerFulfillmentOrderId;
        private final List<OrderItem> items;

        public OrderDetails(String orderId, String sellerFulfillmentOrderId, List<OrderItem> items) {
            this.orderId = orderId;
            this.sellerFulfillmentOrderId = sellerFulfillmentOrderId;
            this.items = items != null ? items : Collections.emptyList();
        }

        public static OrderDetails empty(String orderId) {
            return new OrderDetails(orderId, null, Collections.emptyList());
        }

        public String getOrderId() {
            return orderId;
        }

        public String getSellerFulfillmentOrderId() {
            return sellerFulfillmentOrderId;
        }

        public List<OrderItem> getItems() {
            return items;
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }
    }

    public static class OrderItem {
        private final String sku;
        private final int quantity;

        public OrderItem(String sku, int quantity) {
            this.sku = sku;
            this.quantity = quantity;
        }

        public String getSku() {
            return sku;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    /**
     * Exception thrown when Order Management Service is unavailable or returns an error
     */
    public static class OrderManagementServiceException extends RuntimeException {
        public OrderManagementServiceException(String message) {
            super(message);
        }

        public OrderManagementServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
