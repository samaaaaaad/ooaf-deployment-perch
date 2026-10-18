package com.example.event_ticketing.repository;

import com.example.event_ticketing.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findAllByBookingBookingIdOrderByIssuedAtAscTicketIdAsc(UUID bookingId);
}
