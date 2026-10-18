export default function BookingTickets({ tickets = [] }) {
  return (
    <div aria-label="E-tickets">
      {tickets.map((ticket) => (
        <article key={ticket.ticketId} style={{ marginTop: '1rem', overflowWrap: 'anywhere' }}>
          <h3>Ticket · Seat {ticket.seatLabel}</h3>
          <img src={ticket.qrCodeDataUrl} alt={`Ticket QR code for seat ${ticket.seatLabel}`} width="180" height="180" />
          <p>Ticket code: <code>{ticket.ticketCode}</code></p>
        </article>
      ))}
    </div>
  );
}
