package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.BookingDTO;
import com.example.event_ticketing.model.Booking;
import com.example.event_ticketing.model.EventSeat;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * US 10: emails confirmed e-tickets to the customer's registered address.
 * JavaMailSender only exists when spring.mail.* is configured, so it is injected
 * optionally — with no mail config the send is skipped (and ticket codes are logged
 * for local demos) and booking still succeeds. Send failures are swallowed so a
 * flaky mailbox never fails a paid booking.
 */
@Service
public class BookingEmailService {

    private static final Logger log = LoggerFactory.getLogger(BookingEmailService.class);

    private final JavaMailSender mailSender; // null when spring.mail.* is not set
    private final TicketService ticketService;
    private final String fromAddress;

    public BookingEmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            TicketService ticketService,
            @Value("${perch.mail.from:no-reply@perch.local}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.ticketService = ticketService;
        this.fromAddress = fromAddress;
    }

    public void sendBookingConfirmation(Booking booking, List<EventSeat> seats) {
        List<BookingDTO.Ticket> tickets = ticketService.getTickets(booking.getBookingId());
        if (mailSender == null) {
            log.info(
                    "Mail not configured; skipping e-ticket email for booking {} (tickets: {})",
                    booking.getConfirmationCode(),
                    tickets.stream()
                            .map(t -> t.seatLabel() + "=" + t.ticketCode())
                            .collect(Collectors.joining(", "))
            );
            return;
        }
        String to = booking.getCustomer().getEmail();
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Your Perch e-tickets — " + booking.getConfirmationCode());
            helper.setText(buildPlainText(booking, seats, tickets), buildHtml(booking, seats, tickets));

            for (int i = 0; i < tickets.size(); i++) {
                BookingDTO.Ticket ticket = tickets.get(i);
                byte[] png = ticketService.qrPng(ticket.ticketCode());
                helper.addInline("ticket-qr-" + i, new ByteArrayResource(png) {
                    @Override
                    public String getFilename() {
                        return "ticket-" + ticket.seatLabel() + ".png";
                    }
                }, "image/png");
            }

            mailSender.send(message);
            log.info("Sent e-tickets for booking {} to {}", booking.getConfirmationCode(), to);
        } catch (Exception e) {
            // best-effort: never fail a confirmed booking because email bounced
            log.warn("Could not send e-ticket email for booking {}: {}", booking.getConfirmationCode(), e.getMessage());
        }
    }

    private static String buildPlainText(Booking booking, List<EventSeat> seats, List<BookingDTO.Ticket> tickets) {
        String seatList = seats.stream()
                .map(s -> s.getLabel() + " (" + s.getTier() + ") $" + booking.pricePaidFor(s))
                .collect(Collectors.joining("\n  "));
        String ticketList = tickets.stream()
                .map(t -> "Seat " + t.seatLabel() + " — code " + t.ticketCode())
                .collect(Collectors.joining("\n  "));
        return "Hi " + booking.getCustomer().getFullName() + ",\n\n"
                + "Your booking for \"" + booking.getEvent().getTitle() + "\" is confirmed.\n\n"
                + "Confirmation code: " + booking.getConfirmationCode() + "\n"
                + "Seats:\n  " + seatList + "\n\n"
                + "Your e-tickets (codes stay the same whenever you re-open them):\n  " + ticketList + "\n\n"
                + "Total paid: $" + booking.getTotalAmount()
                + (booking.getStudentDiscountPercent() != null
                        ? " (includes " + booking.getStudentDiscountPercent() + "% student discount)" : "")
                + "\n\n"
                + "Open My Tickets in the Perch app anytime to view the scannable QR codes.\n\n"
                + "See you there,\nThe Perch team";
    }

    private static String buildHtml(Booking booking, List<EventSeat> seats, List<BookingDTO.Ticket> tickets) {
        StringBuilder html = new StringBuilder();
        html.append("<p>Hi ").append(escape(booking.getCustomer().getFullName())).append(",</p>");
        html.append("<p>Your booking for <strong>")
                .append(escape(booking.getEvent().getTitle()))
                .append("</strong> is confirmed.</p>");
        html.append("<p>Confirmation code: <code>")
                .append(escape(booking.getConfirmationCode()))
                .append("</code></p>");
        html.append("<p>Seats:</p><ul>");
        for (EventSeat seat : seats) {
            html.append("<li>Seat ")
                    .append(escape(seat.getLabel()))
                    .append(" (")
                    .append(escape(String.valueOf(seat.getTier())))
                    .append(") $")
                    .append(booking.pricePaidFor(seat))
                    .append("</li>");
        }
        html.append("</ul>");
        html.append("<p><strong>Your e-tickets</strong> — codes never change when you re-view them:</p>");
        for (int i = 0; i < tickets.size(); i++) {
            BookingDTO.Ticket ticket = tickets.get(i);
            html.append("<div style=\"margin:1rem 0;padding:1rem;border:1px solid #ddd;border-radius:8px;\">");
            html.append("<p style=\"margin:0 0 0.5rem;\"><strong>Seat ")
                    .append(escape(ticket.seatLabel()))
                    .append("</strong></p>");
            html.append("<p style=\"margin:0 0 0.5rem;\">Ticket code: <code>")
                    .append(escape(ticket.ticketCode()))
                    .append("</code></p>");
            html.append("<img src=\"cid:ticket-qr-")
                    .append(i)
                    .append("\" alt=\"QR for seat ")
                    .append(escape(ticket.seatLabel()))
                    .append("\" width=\"180\" height=\"180\"/>");
            html.append("</div>");
        }
        html.append("<p>Total paid: <strong>$")
                .append(booking.getTotalAmount())
                .append("</strong>");
        if (booking.getStudentDiscountPercent() != null) {
            html.append(" (includes ")
                    .append(booking.getStudentDiscountPercent())
                    .append("% student discount)");
        }
        html.append("</p>");
        html.append("<p>You can also open <strong>My Tickets</strong> in the Perch app anytime.</p>");
        html.append("<p>See you there,<br/>The Perch team</p>");
        return html.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
