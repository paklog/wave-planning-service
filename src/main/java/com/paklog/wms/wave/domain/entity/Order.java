package com.paklog.wms.wave.domain.entity;

import com.paklog.wms.wave.domain.valueobject.WavePriority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Simplified order aggregate used by the optimization service.
 * Represents the minimal set of data points required by the algorithms.
 */
public class Order {

    private final String orderId;
    private final WavePriority priority;
    private final LocalDateTime requiredDate;
    private final LocalDateTime orderDate;
    private final List<OrderLine> orderLines;
    private final ShippingMethod shippingMethod;
    private final Map<String, String> attributes;
    private final BigDecimal totalVolume;
    private final BigDecimal totalWeight;

    private Order(Builder builder) {
        this.orderId = builder.orderId;
        this.priority = builder.priority;
        this.requiredDate = builder.requiredDate;
        this.orderDate = builder.orderDate;
        this.orderLines = builder.orderLines;
        this.shippingMethod = builder.shippingMethod;
        this.attributes = builder.attributes;
        this.totalVolume = builder.totalVolume;
        this.totalWeight = builder.totalWeight;
    }



    public String getOrderId() {
        return orderId;
    }

    public WavePriority getPriority() {
        return priority;
    }

    public LocalDateTime getRequiredDate() {
        return requiredDate;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public List<OrderLine> getOrderLines() {
        return Collections.unmodifiableList(orderLines);
    }

    public ShippingMethod getShippingMethod() {
        return shippingMethod;
    }

    public String getAttribute(String key, String defaultValue) {
        return attributes.getOrDefault(key, defaultValue);
    }

    public BigDecimal calculateVolume() {
        return totalVolume;
    }

    public BigDecimal calculateWeight() {
        return totalWeight;
    }

    public Map<String, String> getAttributes() {
        return new HashMap<>(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String orderId;
        private WavePriority priority;
        private LocalDateTime requiredDate;
        private LocalDateTime orderDate;
        private List<OrderLine> orderLines;
        private ShippingMethod shippingMethod;
        private Map<String, String> attributes;
        private BigDecimal totalVolume;
        private BigDecimal totalWeight;

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder priority(WavePriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder requiredDate(LocalDateTime requiredDate) {
            this.requiredDate = requiredDate;
            return this;
        }

        public Builder orderDate(LocalDateTime orderDate) {
            this.orderDate = orderDate;
            return this;
        }

        public Builder orderLines(List<OrderLine> orderLines) {
            this.orderLines = orderLines;
            return this;
        }

        public Builder shippingMethod(ShippingMethod shippingMethod) {
            this.shippingMethod = shippingMethod;
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder totalVolume(BigDecimal totalVolume) {
            this.totalVolume = totalVolume;
            return this;
        }

        public Builder totalWeight(BigDecimal totalWeight) {
            this.totalWeight = totalWeight;
            return this;
        }

        public Order build() {
            return new Order(this);
        }
    }

    public static class OrderLine {
        private final int quantity;

        public OrderLine(int quantity) {
            this.quantity = quantity;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    public static class ShippingMethod {
        private final String carrier;

        public ShippingMethod(String carrier) {
            this.carrier = carrier;
        }

        public String getCarrier() {
            return carrier;
        }
    }
}
