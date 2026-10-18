package com.example.event_ticketing.repository;

import com.example.event_ticketing.model.Booking;
import com.example.event_ticketing.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findAllByCustomerUserIdAndStatusOrderByCreatedAtDesc(UUID customerId, BookingStatus status);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<Booking> findByPaymentReference(String paymentReference);
}
