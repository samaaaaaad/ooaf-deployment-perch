package com.example.event_ticketing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class UpdatePricingRequest {
    public UUID floorPlanId;
    @NotNull(message = "Standard price must be 0 or greater")
    @DecimalMin(value = "0", message = "Standard price must be 0 or greater")
    public BigDecimal standardPrice;
    @DecimalMin(value = "0", message = "Premium price must be 0 or greater")
    public BigDecimal premiumPrice;
    /** US 12: optional; null leaves the current value unchanged. */
    public Boolean universitySpecific;
    /** US 12: optional; null leaves the current value unchanged, 0 removes the discount. */
    @Min(value = 0, message = "Student discount must be between 0 and 100 percent")
    @Max(value = 100, message = "Student discount must be between 0 and 100 percent")
    public Integer studentDiscountPercent;

    public UpdatePricingRequest() {}

    public UpdatePricingRequest(UUID floorPlanId, BigDecimal standardPrice) {
        this.floorPlanId = floorPlanId;
        this.standardPrice = standardPrice;
    }

    public UpdatePricingRequest(UUID floorPlanId, BigDecimal standardPrice, BigDecimal premiumPrice) {
        this.floorPlanId = floorPlanId;
        this.standardPrice = standardPrice;
        this.premiumPrice = premiumPrice;
    }
}
