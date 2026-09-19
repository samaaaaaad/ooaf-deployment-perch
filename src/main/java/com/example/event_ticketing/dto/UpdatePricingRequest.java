package com.example.event_ticketing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class UpdatePricingRequest {
    public UUID floorPlanId;
    public BigDecimal standardPrice;
    public BigDecimal premiumPrice;

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
