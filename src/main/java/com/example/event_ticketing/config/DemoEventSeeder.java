package com.example.event_ticketing.config;

import com.example.event_ticketing.dto.CreateEventRequest;
import com.example.event_ticketing.dto.EventDTO;
import com.example.event_ticketing.dto.FloorPlanDTO;
import com.example.event_ticketing.dto.FloorPlanRequest;
import com.example.event_ticketing.dto.PublishEventRequest;
import com.example.event_ticketing.dto.UpdatePricingRequest;
import com.example.event_ticketing.model.User;
import com.example.event_ticketing.repository.UserRepository;
import com.example.event_ticketing.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seeds the demo organiser's catalogue of published events, so Browse, Event Detail, checkout and
 * My Tickets all run against real database rows. Every event goes through {@link EventService}
 * (create, then price, then publish), so the seed data passes the same rules as organiser input.
 *
 * <p>Dates are relative to today (Sydney time), so the catalogue stays upcoming whenever the app
 * starts. Idempotent: events and floor plans the organiser already has, matched by name, are
 * skipped. Turn it off with {@code perch.seed.demo-events=false} (the test profile does).
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "perch.seed.demo-events", havingValue = "true", matchIfMissing = true)
public class DemoEventSeeder implements CommandLineRunner {

    static final String ORGANISER_EMAIL = "organiser@demo.local";
    static final String CABARET_PLAN = "Cabaret Tables (32 seats)";
    static final String ARENA_PLAN = "Courtside Arena (96 seats)";

    private static final Logger log = LoggerFactory.getLogger(DemoEventSeeder.class);
    private static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");

    private enum Layout { STAGE, CABARET, ARENA }

    private record SeedEvent(
            String title, String category, String venueName, String city, int daysAhead, LocalTime startTime,
            Layout layout, String standardPrice, String premiumPrice, boolean universitySpecific,
            int studentDiscountPercent, int image, String description) {
    }

    private static final List<SeedEvent> EVENTS = List.of(
            new SeedEvent("Sunset Rooftop Sessions", "music", "Rooftop Bar", "Alexandria", 6, LocalTime.of(19, 0),
                    Layout.STAGE, "35.00", "55.00", false, 0, 1,
                    "Three local indie acts play an acoustic set as the sun goes down over the city skyline. "
                            + "Premium rows sit closest to the stage. Bar open from 6pm."),
            new SeedEvent("Late Night Laughs", "comedy", "The Comedy Loft", "Newtown", 9, LocalTime.of(20, 0),
                    Layout.CABARET, "22.00", "32.00", false, 15, 2,
                    "A five-comic line-up of Sydney's sharpest up-and-comers, hosted by a festival regular. "
                            + "Cabaret seating at shared tables; front tables are premium."),
            new SeedEvent("A Midsummer Night's Dream", "theatre", "Old Fitz Theatre", "Woolloomooloo", 14, LocalTime.of(19, 30),
                    Layout.STAGE, "40.00", "60.00", false, 20, 3,
                    "An independent ensemble stages Shakespeare's comedy in a pared-back, 90-minute production. "
                            + "Runs without an interval."),
            new SeedEvent("Indie Film Night: Short Film Showcase", "cinema", "The Vault Cinema", "Surry Hills", 11, LocalTime.of(18, 30),
                    Layout.STAGE, "18.00", "24.00", true, 20, 4,
                    "Eight short films from university film-school graduates, followed by a Q&A with the directors. "
                            + "For verified university students only."),
            new SeedEvent("Inner West Futsal Cup Final", "sports", "Marrickville PCYC", "Marrickville", 20, LocalTime.of(10, 0),
                    Layout.ARENA, "12.00", "18.00", false, 0, 5,
                    "Watch the top two community futsal sides play the season decider. "
                            + "Courtside seats are premium; the sides are standard."),
            new SeedEvent("Natural Wine Tasting", "food", "The Cellar Door", "Chippendale", 18, LocalTime.of(18, 0),
                    Layout.CABARET, "55.00", "70.00", false, 0, 6,
                    "Taste six low-intervention wines from small NSW producers, paired with cheese and charcuterie. "
                            + "18+ only. Front tables get a guided talk from the winemaker."),
            new SeedEvent("Intro to Pottery Workshop", "workshop", "Clay & Co Studio", "Erskineville", 7, LocalTime.of(13, 0),
                    Layout.CABARET, "75.00", "75.00", false, 10, 7,
                    "A hands-on, three-hour beginner wheel-throwing class. All clay, glazes and firing are included, "
                            + "and finished pieces are ready to collect two weeks later."),
            new SeedEvent("Campus Trivia Night", "community", "Manning Bar", "Camperdown", 16, LocalTime.of(19, 0),
                    Layout.CABARET, "10.00", "10.00", true, 0, 8,
                    "Teams of four compete across eight rounds of general knowledge, music and campus trivia. "
                            + "For verified university students only. Book a whole table for your team."),
            new SeedEvent("Jazz & Small Plates", "music", "Basement Bar", "Sydney CBD", 26, LocalTime.of(20, 0),
                    Layout.CABARET, "30.00", "45.00", false, 0, 9,
                    "A quartet plays standards and originals across two sets while the kitchen serves shared plates. "
                            + "Food is ordered at the table."),
            new SeedEvent("Improv Jam Night", "comedy", "Giant Dwarf", "Redfern", 25, LocalTime.of(20, 30),
                    Layout.STAGE, "20.00", "28.00", false, 25, 10,
                    "Long-form improv made up on the spot from audience suggestions. No two nights are the same."),
            new SeedEvent("One-Act Play Festival", "theatre", "Kings Cross Theatre", "Kings Cross", 31, LocalTime.of(19, 0),
                    Layout.STAGE, "28.00", "38.00", false, 25, 11,
                    "Four new one-act plays by emerging Australian writers, each under 25 minutes, in one evening."),
            new SeedEvent("Outdoor Movie: Amélie", "cinema", "Riverside Lawn", "Parramatta", 33, LocalTime.of(19, 45),
                    Layout.STAGE, "15.00", "22.00", false, 0, 12,
                    "The 2001 French classic on a giant inflatable screen by the river. "
                            + "Premium rows include a deck chair and blanket."),
            new SeedEvent("Sunrise Yoga & 5K Run", "sports", "Bondi Pavilion", "Bondi Beach", 34, LocalTime.of(6, 30),
                    Layout.ARENA, "18.00", "25.00", false, 0, 13,
                    "A 30-minute beach yoga flow, then a social 5K along the promenade. "
                            + "Premium spots include a recovery smoothie."),
            new SeedEvent("Dumpling Masterclass", "food", "The Kitchen Studio", "Ashfield", 41, LocalTime.of(11, 0),
                    Layout.CABARET, "65.00", "80.00", false, 15, 14,
                    "Learn to fold four styles of dumpling with a Shanghainese chef, then eat what you make. "
                            + "Vegetarian options are available."),
            new SeedEvent("Film Photography Basics", "workshop", "The Darkroom", "Marrickville", 40, LocalTime.of(14, 0),
                    Layout.CABARET, "80.00", "80.00", true, 20, 15,
                    "Shoot a roll of 35mm film on a guided photo walk, then develop it in the darkroom. "
                            + "A camera loan is included. For verified university students only."),
            new SeedEvent("Local Makers Market", "community", "Leichhardt Town Hall", "Leichhardt", 47, LocalTime.of(9, 0),
                    Layout.ARENA, "0.00", "0.00", false, 0, 16,
                    "Forty independent makers selling ceramics, prints, candles and more. "
                            + "Free entry, but reserve a seat for the 11am live demonstrations.")
    );

    private final UserRepository userRepository;
    private final EventService eventService;
    private final Clock clock;

    public DemoEventSeeder(UserRepository userRepository, EventService eventService, Clock clock) {
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        User organiser = userRepository.findByEmailIgnoreCase(ORGANISER_EMAIL).orElse(null);
        if (organiser == null) {
            log.warn("Demo organiser {} not found; skipping demo events", ORGANISER_EMAIL);
            return;
        }
        UUID organiserId = organiser.getUserId();

        List<FloorPlanDTO> plans = eventService.getAvailableFloorPlans(organiserId);
        UUID stage = plans.stream()
                .filter(p -> "STAGE".equalsIgnoreCase(p.layoutType) && p.organiserId == null)
                .map(p -> p.floorPlanId)
                .findFirst()
                .orElse(null);
        UUID cabaret = findOrCreatePlan(organiserId, plans, CABARET_PLAN, cabaretLayout());
        UUID arena = findOrCreatePlan(organiserId, plans, ARENA_PLAN, arenaLayout());

        Set<String> existing = eventService.getEventsForOrganiser(organiserId).stream()
                .map(e -> e.title)
                .collect(Collectors.toSet());
        int created = 0;
        for (SeedEvent seed : EVENTS) {
            if (existing.contains(seed.title())) {
                continue;
            }
            UUID planId = switch (seed.layout()) {
                case STAGE -> stage;
                case CABARET -> cabaret;
                case ARENA -> arena;
            };
            if (planId == null) {
                continue; // no built-in Stage plan in this database; skip rather than fail startup
            }
            publish(organiserId, seed, planId);
            created++;
        }
        if (created > 0) {
            log.info("Seeded {} demo events for {}", created, ORGANISER_EMAIL);
        }
    }

    private void publish(UUID organiserId, SeedEvent seed, UUID floorPlanId) {
        CreateEventRequest create = new CreateEventRequest();
        create.title = seed.title();
        create.description = seed.description();
        create.category = seed.category();
        create.venueName = seed.venueName();
        create.city = seed.city();
        create.universitySpecific = seed.universitySpecific();
        create.startsAt = LocalDate.now(clock.withZone(SYDNEY))
                .plusDays(seed.daysAhead())
                .atTime(seed.startTime())
                .atZone(SYDNEY)
                .toInstant()
                .toString();
        EventDTO event = eventService.createEvent(organiserId, create);

        UpdatePricingRequest pricing = new UpdatePricingRequest(
                floorPlanId, new BigDecimal(seed.standardPrice()), new BigDecimal(seed.premiumPrice()));
        pricing.studentDiscountPercent = seed.studentDiscountPercent();
        eventService.updateEventPricing(organiserId, event.eventId, pricing);

        eventService.publishEvent(organiserId, event.eventId,
                new PublishEventRequest("/events/event-" + seed.image() + ".jpg"));
    }

    private UUID findOrCreatePlan(UUID organiserId, List<FloorPlanDTO> plans, String name, LayoutDraft layout) {
        return plans.stream()
                .filter(p -> name.equals(p.name))
                .map(p -> p.floorPlanId)
                .findFirst()
                .orElseGet(() -> {
                    FloorPlanRequest request = new FloorPlanRequest();
                    request.name = name;
                    request.layoutType = "CUSTOM";
                    request.capacity = layout.seats();
                    request.layoutJson = layout.json();
                    return eventService.createFloorPlan(organiserId, request).floorPlanId;
                });
    }

    private record LayoutDraft(String json, int seats) {
    }

    /** A stage across the top with eight four-seat tables in two rows; the front row is premium. */
    static LayoutDraft cabaretLayout() {
        List<String> items = new ArrayList<>();
        items.add(item(items.size() + 1, "stage", 290, 24, null));
        int seats = 0;
        int[] rows = {200, 380};
        for (int row = 0; row < rows.length; row++) {
            String tier = row == 0 ? "PREMIUM" : "STANDARD";
            for (int col = 0; col < 4; col++) {
                int cx = 150 + col * 200;
                int cy = rows[row];
                items.add(item(items.size() + 1, "table", cx, cy, null));
                int[][] offsets = {{0, -44}, {44, 0}, {0, 44}, {-44, 0}};
                for (int[] o : offsets) {
                    items.add(item(items.size() + 1, "seat", cx + o[0], cy + o[1], tier));
                    seats++;
                }
            }
        }
        return new LayoutDraft("[" + String.join(",", items) + "]", seats);
    }

    /** A court marked by a barrier in the middle, with three rows of seats along each side. */
    static LayoutDraft arenaLayout() {
        List<String> items = new ArrayList<>();
        items.add(item(items.size() + 1, "barrier", 375, 272, null));
        int seats = 0;
        int[] rowsY = {80, 115, 150, 410, 445, 480};
        for (int y : rowsY) {
            String tier = y == 150 || y == 410 ? "PREMIUM" : "STANDARD";
            for (int col = 0; col < 16; col++) {
                items.add(item(items.size() + 1, "seat", 150 + col * 40, y, tier));
                seats++;
            }
        }
        return new LayoutDraft("[" + String.join(",", items) + "]", seats);
    }

    private static String item(int index, String type, int x, int y, String tier) {
        String base = "{\"id\":\"seed-" + index + "\",\"type\":\"" + type + "\",\"x\":" + x + ",\"y\":" + y;
        return tier == null ? base + "}" : base + ",\"tier\":\"" + tier + "\"}";
    }
}
