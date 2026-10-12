package com.example.event_ticketing.service;

import com.example.event_ticketing.model.Booking;
import com.example.event_ticketing.model.EventSeat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Sends booking confirmation emails. JavaMailSender only exists when spring.mail.*
 * is configured, so this is injected optionally: with no mail config the method
 * is a no-op and booking still succeeds. Any send failure is swallowed so a flaky
 * mailbox never fails a paid booking.
 */
@Service
public class BookingEmailService {

    private static final Logger log = LoggerFactory.getLogger(BookingEmailService.class);

    private final JavaMailSender mailSender; // null when spring.mail.* is not set
    private final String fromAddress;

    public BookingEmailService(
            @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender,
            @Value("${perch.mail.from:no-reply@perch.local}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendBookingConfirmation(Booking booking, List<EventSeat> seats) {
        if (mailSender == null) {
            log.info("Mail not configured; skipping confirmation email for booking {}", booking.getConfirmationCode());
            return;
        }
        String to = booking.getCustomer().getEmail();
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            String seatList = seats.stream()
                    .map(s -> s.getLabel() + " (" + s.getTier() + ") $" + booking.pricePaidFor(s))
                    .collect(Collectors.joining("\n  "));
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject("Your Perch booking " + booking.getConfirmationCode());
            message.setText(
                    "Hi " + booking.getCustomer().getFullName() + ",\n\n"
                            + "Your booking for \"" + booking.getEvent().getTitle() + "\" is confirmed.\n\n"
                            + "Confirmation code: " + booking.getConfirmationCode() + "\n"
                            + "Seats:\n  " + seatList + "\n\n"
                            + "Total paid: $" + booking.getTotalAmount()
                            + (booking.getStudentDiscountPercent() != null
                                    ? " (includes " + booking.getStudentDiscountPercent() + "% student discount)" : "")
                            + "\n\n"
                            + "See you there,\nThe Perch team"
            );
            mailSender.send(message);
            log.info("Sent booking confirmation {} to {}", booking.getConfirmationCode(), to);
        } catch (Exception e) {
            // best-effort: never fail a confirmed booking because email bounced
            log.warn("Could not send confirmation email for booking {}: {}", booking.getConfirmationCode(), e.getMessage());
        }
    }
}
