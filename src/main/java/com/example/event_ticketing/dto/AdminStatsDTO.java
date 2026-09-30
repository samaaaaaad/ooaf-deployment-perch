package com.example.event_ticketing.dto;

import java.math.BigDecimal;
import java.util.List;

public class AdminStatsDTO {
    public BigDecimal totalRevenue;
    public List<PopularEventDTO> popularEventsByTicketsSold;

    public AdminStatsDTO() {}

    public AdminStatsDTO(BigDecimal totalRevenue, List<PopularEventDTO> popularEventsByTicketsSold) {
        this.totalRevenue = totalRevenue;
        this.popularEventsByTicketsSold = popularEventsByTicketsSold;
    }
}
