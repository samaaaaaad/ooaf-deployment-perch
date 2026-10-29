package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.AdminStatsDTO;
import com.example.event_ticketing.dto.PopularEventDTO;
import com.example.event_ticketing.repository.EventSeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTests {

    @Mock
    private EventSeatRepository eventSeatRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void combinesTotalRevenueWithTheMostPopularEvents() {
        PopularEventDTO jazz = new PopularEventDTO(UUID.randomUUID(), "Jazz & Small Plates", 12L, new BigDecimal("360.00"));
        when(eventSeatRepository.sumRevenue()).thenReturn(new BigDecimal("420.00"));
        when(eventSeatRepository.findPopularEventsByTicketsSold()).thenReturn(List.of(jazz));

        AdminStatsDTO stats = adminService.getStats();

        assertThat(stats.totalRevenue).isEqualByComparingTo("420.00");
        assertThat(stats.popularEventsByTicketsSold).containsExactly(jazz);
    }

    @Test
    void reportsNoSalesWhenNothingHasBeenBooked() {
        when(eventSeatRepository.sumRevenue()).thenReturn(BigDecimal.ZERO);
        when(eventSeatRepository.findPopularEventsByTicketsSold()).thenReturn(List.of());

        AdminStatsDTO stats = adminService.getStats();

        assertThat(stats.totalRevenue).isEqualByComparingTo("0");
        assertThat(stats.popularEventsByTicketsSold).isEmpty();
    }
}
