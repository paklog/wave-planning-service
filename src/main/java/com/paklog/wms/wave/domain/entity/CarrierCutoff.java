package com.paklog.wms.wave.domain.entity;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Entity representing carrier cutoff times
 * Defines when a carrier must receive shipments to meet service level agreements
 */
public class CarrierCutoff {

    private String carrierId;
    private String carrierName;
    private LocalTime cutoffTime;
    private String serviceLevel;
    private Boolean isActive;

    public CarrierCutoff() {
        // Default constructor for MongoDB
        this.isActive = true;
    }

    public CarrierCutoff(String carrierId, String carrierName, LocalTime cutoffTime, String serviceLevel) {
        this.carrierId = Objects.requireNonNull(carrierId, "Carrier ID cannot be null");
        this.carrierName = Objects.requireNonNull(carrierName, "Carrier name cannot be null");
        this.cutoffTime = Objects.requireNonNull(cutoffTime, "Cutoff time cannot be null");
        this.serviceLevel = serviceLevel;
        this.isActive = true;
    }

    public boolean isCutoffPassed(LocalTime currentTime) {
        return currentTime.isAfter(cutoffTime);
    }

    public boolean isCutoffApproaching(LocalTime currentTime, int warningMinutes) {
        LocalTime warningTime = cutoffTime.minusMinutes(warningMinutes);
        return currentTime.isAfter(warningTime) && currentTime.isBefore(cutoffTime);
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    // Getters and setters
    public String getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(String carrierId) {
        this.carrierId = carrierId;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public LocalTime getCutoffTime() {
        return cutoffTime;
    }

    public void setCutoffTime(LocalTime cutoffTime) {
        this.cutoffTime = cutoffTime;
    }

    public String getServiceLevel() {
        return serviceLevel;
    }

    public void setServiceLevel(String serviceLevel) {
        this.serviceLevel = serviceLevel;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarrierCutoff that = (CarrierCutoff) o;
        return Objects.equals(carrierId, that.carrierId) &&
                Objects.equals(serviceLevel, that.serviceLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(carrierId, serviceLevel);
    }

    @Override
    public String toString() {
        return "CarrierCutoff{" +
                "carrierId='" + carrierId + '\'' +
                ", carrierName='" + carrierName + '\'' +
                ", cutoffTime=" + cutoffTime +
                ", serviceLevel='" + serviceLevel + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
