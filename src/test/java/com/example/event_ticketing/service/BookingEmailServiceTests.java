package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.BookingDTO;
import com.example.event_ticketing.model.Booking;
import com.example.event_ticketing.model.BookingStatus;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.SeatTier;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEmailServiceTests {

    @Mock private JavaMailSender mailSender;
    @Mock private TicketService ticketService;

    private BookingEmailService emailService;
    private Booking booking;
    private EventSeat seat;
    private BookingDTO.Ticket ticketDto;

    @BeforeEach
    void setUp() {
        emailService = new BookingEmailService(mailSender, ticketService, "no-reply@perch.local");

        User customer = new User("Sam Buyer", "sam@example.com", "hash", UserRole.CUSTOMER);
        customer.setUserId(UUID.randomUUID());
        User organiser = new User("Org", "org@example.com", "hash", UserRole.ORGANISER);
        Event event = new Event(organiser, "Jazz Night", null);
        booking = new Booking(customer, event, new BigDecimal("40.00"), "PERCH-ABC", BookingStatus.CONFIRMED);
        ReflectionTestUtils.setField(booking, "bookingId", UUID.randomUUID());

        seat = new EventSeat();
        seat.setLabel("A1");
        seat.setTier(SeatTier.STANDARD);
        seat.setPrice(new BigDecimal("40.00"));

        ticketDto = new BookingDTO.Ticket(
                UUID.randomUUID(),
                "A1",
                "ticket-code-stable-1",
                "data:image/png;base64,AAA"
        );
    }

    @Test
    void emailsStableTicketCodeAndDoesNotRequireRegeneratingCodes() throws Exception {
        when(ticketService.getTickets(booking.getBookingId())).thenReturn(List.of(ticketDto));
        when(ticketService.qrPng("ticket-code-stable-1")).thenReturn(new byte[] {1, 2, 3, 4});

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendBookingConfirmation(booking, List.of(seat));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();

        assertThat(sent.getSubject()).contains("e-tickets").contains("PERCH-ABC");
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("sam@example.com");

        Object content = sent.getContent();
        assertThat(content).isInstanceOf(MimeMultipart.class);
        String body = flatten((MimeMultipart) content);
        assertThat(body).contains("Jazz Night");
        assertThat(body).contains("ticket-code-stable-1");
        assertThat(body).contains("Seat A1");
        assertThat(body).contains("cid:ticket-qr-0");
        assertThat(body).contains("My Tickets");

        // QR is rendered from the persisted code — the code itself is never reissued here.
        verify(ticketService).qrPng("ticket-code-stable-1");
        verify(ticketService, never()).issue(any(), any());
    }

    @Test
    void skipsSendWhenMailIsNotConfiguredButStillLoadsTickets() {
        BookingEmailService noMail = new BookingEmailService(null, ticketService, "no-reply@perch.local");
        when(ticketService.getTickets(booking.getBookingId())).thenReturn(List.of(ticketDto));

        noMail.sendBookingConfirmation(booking, List.of(seat));

        verify(ticketService).getTickets(booking.getBookingId());
    }

    private static String flatten(MimeMultipart multipart) throws Exception {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            Object part = multipart.getBodyPart(i).getContent();
            if (part instanceof String text) {
                out.append(text);
            } else if (part instanceof MimeMultipart nested) {
                out.append(flatten(nested));
            }
        }
        return out.toString();
    }
}
