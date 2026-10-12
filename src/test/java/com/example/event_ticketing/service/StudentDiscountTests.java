package com.example.event_ticketing.service;

import com.example.event_ticketing.dto.BookingDTO;
import com.example.event_ticketing.dto.CheckoutRequest;
import com.example.event_ticketing.dto.CreateEventRequest;
import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.dto.UpdatePricingRequest;
import com.example.event_ticketing.dto.UserDTO;
import com.example.event_ticketing.exception.ValidationException;
import com.example.event_ticketing.model.Event;
import com.example.event_ticketing.model.EventSeat;
import com.example.event_ticketing.model.EventStatus;
import com.example.event_ticketing.model.SeatTier;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.model.UserRole;
import com.example.event_ticketing.payment.PaymentProvider;
import com.example.event_ticketing.payment.PaymentResult;
import com.example.event_ticketing.repository.BookingRepository;
import com.example.event_ticketing.repository.EventRepository;
import com.example.event_ticketing.repository.EventSeatRepository;
import com.example.event_ticketing.repository.FloorPlanRepository;
import com.example.event_ticketing.repository.SeatRepository;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.util.StudentEligibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US 12: university-specific / student-discounted events. */
@ExtendWith(MockitoExtension.class)
class StudentDiscountTests {

    private static final Instant NOW = Instant.parse("2026-10-10T00:00:00Z");

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private FloorPlanRepository floorPlanRepository;
    @Mock private EventSeatRepository eventSeatRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentProvider paymentProvider;
    @Mock private BookingEmailService emailService;

    private EventService eventService;
    private BookingService bookingService;
    private SeatHoldService seatHoldService;
    private User organiser;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, userRepository, floorPlanRepository, eventSeatRepository, seatRepository);
        bookingService = new BookingService(eventSeatRepository, bookingRepository, userRepository,
                paymentProvider, emailService, Clock.fixed(NOW, ZoneOffset.UTC));
        seatHoldService = new SeatHoldService(eventSeatRepository, userRepository,
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(5));
        organiser = new User("Owner", "owner@example.com", "hash", UserRole.ORGANISER);
        organiser.setUserId(UUID.randomUUID());
    }

    @Test
    void recognisesUniversityEmailDomains() {
        assertThat(StudentEligibility.hasUniversityEmail("alex@uni.sydney.edu.au")).isTrue();
        assertThat(StudentEligibility.hasUniversityEmail("ALEX@MIT.EDU")).isTrue();
        assertThat(StudentEligibility.hasUniversityEmail("alex@gmail.com")).isFalse();
        assertThat(StudentEligibility.hasUniversityEmail("edu.au@gmail.com")).isFalse();
        assertThat(StudentEligibility.hasUniversityEmail(null)).isFalse();
    }

    @Test
    void aUniversityEmailAloneDoesNotMakeSomeoneAStudent() {
        User unverified = unverifiedStudent();
        User verified = student();

        assertThat(StudentEligibility.isEligible(unverified)).isFalse();
        assertThat(StudentEligibility.isEligible(verified)).isTrue();
        assertThat(UserDTO.from(unverified).studentEligible).isFalse();
        assertThat(UserDTO.from(unverified).universityEmail).isTrue();
        assertThat(UserDTO.from(verified).studentEligible).isTrue();
    }

    @Test
    void unverifiedUniversityEmailPaysTheStandardTotal() {
        BookingDTO booking = checkout(unverifiedStudent(), 20);

        assertThat(booking.studentDiscountPercent).isNull();
        assertThat(booking.totalAmount).isEqualByComparingTo("65.00");
    }

    @Test
    void appliesDiscountRoundedToCents() {
        assertThat(StudentEligibility.applyDiscount(new BigDecimal("25.00"), 20)).isEqualByComparingTo("20.00");
        assertThat(StudentEligibility.applyDiscount(new BigDecimal("9.99"), 15)).isEqualByComparingTo("8.49");
        assertThat(StudentEligibility.applyDiscount(new BigDecimal("25.00"), null)).isEqualByComparingTo("25.00");
    }

    @Test
    void createsAnEventMarkedUniversitySpecific() {
        when(userRepository.findById(organiser.getUserId())).thenReturn(Optional.of(organiser));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateEventRequest request = new CreateEventRequest();
        request.title = "O-Week Party";
        request.universitySpecific = true;

        EventDTO created = eventService.createEvent(organiser.getUserId(), request);

        assertThat(created.universitySpecific).isTrue();
    }

    @Test
    void savesStudentDiscountAndExposesBothPrices() {
        Event event = draftEvent();
        when(eventRepository.findByEventIdAndOrganiserUserId(event.getEventId(), organiser.getUserId()))
                .thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        UpdatePricingRequest request = new UpdatePricingRequest(null, new BigDecimal("25.00"), new BigDecimal("40.00"));
        request.universitySpecific = true;
        request.studentDiscountPercent = 20;

        EventDTO dto = eventService.updateEventPricing(organiser.getUserId(), event.getEventId(), request);

        assertThat(dto.universitySpecific).isTrue();
        assertThat(dto.studentDiscountPercent).isEqualTo(20);
        assertThat(dto.standardPrice).isEqualByComparingTo("25.00");
        assertThat(dto.studentStandardPrice).isEqualByComparingTo("20.00");
        assertThat(dto.studentPremiumPrice).isEqualByComparingTo("32.00");
    }

    @Test
    void rejectsAStudentDiscountOutsideZeroToHundred() {
        UpdatePricingRequest request = new UpdatePricingRequest(null, new BigDecimal("25.00"));
        request.studentDiscountPercent = 101;

        assertThatThrownBy(() -> eventService.updateEventPricing(organiser.getUserId(), UUID.randomUUID(), request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Student discount must be between 0 and 100 percent");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void filtersPublishedEventsByUniversityAndDiscount() {
        Event plain = publishedEvent("Plain", false, null);
        Event uniOnly = publishedEvent("Uni", true, null);
        Event discounted = publishedEvent("Discounted", false, 10);
        Event both = publishedEvent("Both", true, 15);
        when(eventRepository.findAllByStatusOrderByCreatedAtDesc(EventStatus.PUBLISHED))
                .thenReturn(List.of(plain, uniOnly, discounted, both));

        assertThat(eventService.getPublishedEvents(false, false)).extracting(e -> e.title)
                .containsExactly("Plain", "Uni", "Discounted", "Both");
        assertThat(eventService.getPublishedEvents(true, false)).extracting(e -> e.title)
                .containsExactly("Uni", "Both");
        assertThat(eventService.getPublishedEvents(false, true)).extracting(e -> e.title)
                .containsExactly("Discounted", "Both");
        assertThat(eventService.getPublishedEvents(true, true)).extracting(e -> e.title)
                .containsExactly("Both");
    }

    @Test
    void eligibleStudentPaysTheDiscountedTotal() {
        User student = student();
        BookingDTO booking = checkout(student, 20);

        assertThat(booking.studentDiscountPercent).isEqualTo(20);
        assertThat(booking.totalAmount).isEqualByComparingTo("52.00"); // (25 + 40) * 0.8
        assertThat(booking.seats).extracting(s -> s.price.toPlainString()).containsExactly("20.00", "32.00");
        verify(paymentProvider).charge(eq(new BigDecimal("52.00")), eq("AUD"), anyString(), anyString());
    }

    @Test
    void nonStudentPaysTheStandardTotal() {
        User regular = regular();
        BookingDTO booking = checkout(regular, 20);

        assertThat(booking.studentDiscountPercent).isNull();
        assertThat(booking.totalAmount).isEqualByComparingTo("65.00");
    }

    @Test
    void nonStudentCannotHoldASeatAtAStudentsOnlyEvent() {
        User regular = regular();
        EventSeat seat = new EventSeat(publishedEvent("O-Week", true, null), "S1", SeatTier.STANDARD, new BigDecimal("25.00"), 0, 0);
        seat.setEventSeatId(UUID.randomUUID());
        when(userRepository.findById(regular.getUserId())).thenReturn(Optional.of(regular));
        when(eventSeatRepository.findByEventSeatIdAndEventEventId(seat.getEventSeatId(), seat.getEvent().getEventId()))
                .thenReturn(Optional.of(seat));

        assertThatThrownBy(() -> seatHoldService.holdSeat(seat.getEvent().getEventId(), seat.getEventSeatId(), regular.getUserId()))
                .isInstanceOf(ValidationException.class)
                .hasMessage(StudentEligibility.STUDENTS_ONLY_MESSAGE);
        verify(eventSeatRepository, never()).saveAndFlush(any());
    }

    @Test
    void studentCanHoldASeatAtAStudentsOnlyEvent() {
        User student = student();
        EventSeat seat = new EventSeat(publishedEvent("O-Week", true, null), "S1", SeatTier.STANDARD, new BigDecimal("25.00"), 0, 0);
        seat.setEventSeatId(UUID.randomUUID());
        when(userRepository.findById(student.getUserId())).thenReturn(Optional.of(student));
        when(eventSeatRepository.findByEventSeatIdAndEventEventId(seat.getEventSeatId(), seat.getEvent().getEventId()))
                .thenReturn(Optional.of(seat));

        seatHoldService.holdSeat(seat.getEvent().getEventId(), seat.getEventSeatId(), student.getUserId());

        assertThat(seat.getHeldBy()).isEqualTo(student);
    }

    @Test
    void nonStudentCannotCheckOutAStudentsOnlyEvent() {
        User regular = regular();

        assertThatThrownBy(() -> checkout(regular, null, true))
                .isInstanceOf(ValidationException.class)
                .hasMessage(StudentEligibility.STUDENTS_ONLY_MESSAGE);
        verify(paymentProvider, never()).charge(any(), anyString(), anyString(), anyString());
        verify(bookingRepository, never()).save(any());
    }

    private BookingDTO checkout(User customer, Integer discountPercent) {
        return checkout(customer, discountPercent, false);
    }

    private BookingDTO checkout(User customer, Integer discountPercent, boolean studentsOnly) {
        Event event = publishedEvent("Gig", studentsOnly, discountPercent);
        EventSeat standard = heldSeat(event, "S1", SeatTier.STANDARD, "25.00", customer);
        EventSeat premium = heldSeat(event, "S2", SeatTier.PREMIUM, "40.00", customer);
        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.of(customer));
        when(eventSeatRepository.findByEventSeatIdAndEventEventId(standard.getEventSeatId(), event.getEventId()))
                .thenReturn(Optional.of(standard));
        when(eventSeatRepository.findByEventSeatIdAndEventEventId(premium.getEventSeatId(), event.getEventId()))
                .thenReturn(Optional.of(premium));
        if (!studentsOnly || StudentEligibility.isEligible(customer)) {
            when(paymentProvider.charge(any(), anyString(), anyString(), anyString()))
                    .thenReturn(PaymentResult.approved("txn_1"));
        }

        CheckoutRequest request = new CheckoutRequest();
        request.customerId = customer.getUserId();
        request.seatIds = List.of(standard.getEventSeatId(), premium.getEventSeatId());
        return bookingService.checkout(event.getEventId(), request);
    }

    private Event draftEvent() {
        Event event = new Event(organiser, "Gig", null);
        event.setEventId(UUID.randomUUID());
        return event;
    }

    private Event publishedEvent(String title, boolean universitySpecific, Integer discountPercent) {
        Event event = new Event(organiser, title, null);
        event.setEventId(UUID.randomUUID());
        event.setStatus(EventStatus.PUBLISHED);
        event.setStandardPrice(new BigDecimal("25.00"));
        event.setPremiumPrice(new BigDecimal("40.00"));
        event.setUniversitySpecific(universitySpecific);
        event.setStudentDiscountPercent(discountPercent);
        return event;
    }

    private User student() {
        User user = customer("sam@student.uts.edu.au");
        user.setStudentVerified(true);
        return user;
    }

    private User unverifiedStudent() {
        return customer("sam@student.uts.edu.au");
    }

    private User regular() {
        return customer("sam@gmail.com");
    }

    private User customer(String email) {
        User user = new User("Sam", email, "hash", UserRole.CUSTOMER);
        user.setUserId(UUID.randomUUID());
        return user;
    }

    private EventSeat heldSeat(Event event, String label, SeatTier tier, String price, User customer) {
        EventSeat seat = new EventSeat(event, label, tier, new BigDecimal(price), 0, 0);
        seat.setEventSeatId(UUID.randomUUID());
        seat.hold(customer, NOW.plusSeconds(300));
        return seat;
    }
}
