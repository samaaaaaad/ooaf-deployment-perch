package com.example.event_ticketing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class UpdatePricingRequest {
    public UUID floorPlanId;
    public BigDecimal standardPrice;
    public BigDecimal premiumPrice;
    /** US 12: optional; null leaves the current value unchanged. */
    public Boolean universitySpecific;
    /** US 12: optional; null leaves the current value unchanged, 0 removes the discount. */
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
