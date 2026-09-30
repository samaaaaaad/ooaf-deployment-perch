package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.AdminStatsDTO;
import com.example.event_ticketing.repository.EventSeatRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final EventSeatRepository eventSeatRepository;

    public AdminService(EventSeatRepository eventSeatRepository) {
        this.eventSeatRepository = eventSeatRepository;
    }

    public AdminStatsDTO getStats() {
        return new AdminStatsDTO(
                eventSeatRepository.sumRevenue(),
                eventSeatRepository.findPopularEventsByTicketsSold()
        );
    }
}
