package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.BookingDTO;
import com.example.event_ticketing.model.Booking;
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.Ticket;
import com.example.event_ticketing.repository.TicketRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {
    private final TicketRepository repository;
    private final Clock clock;

    public TicketService(TicketRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Called only inside the booking confirmation transaction. */
    public void issue(Booking booking, List<EventSeat> seats) {
        for (EventSeat seat : seats) {
            repository.save(new Ticket(booking, seat, clock.instant()));
        }
        repository.flush();
    }

    public List<BookingDTO.Ticket> getTickets(UUID bookingId) {
        return repository.findAllByBookingBookingIdOrderByIssuedAtAscTicketIdAsc(bookingId).stream()
                .map(ticket -> new BookingDTO.Ticket(ticket.getTicketId(), ticket.getSeat().getLabel(),
                        ticket.getTicketCode(), qrDataUrl(ticket.getTicketCode())))
                .toList();
    }

    /** PNG bytes for the same durable ticket code (used by in-app QR and email embeds). */
    public byte[] qrPng(String ticketCode) {
        try {
            var matrix = new QRCodeWriter().encode(ticketCode, BarcodeFormat.QR_CODE, 240, 240);
            var output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate ticket QR code", e);
        }
    }

    private String qrDataUrl(String code) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrPng(code));
    }
}
