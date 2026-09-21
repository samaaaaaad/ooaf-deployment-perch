package com.example.event_ticketing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * US 7.2 - periodically returns seats with expired holds to AVAILABLE.
 *
 * Correctness does not depend on this job: SeatHoldService.holdSeat already treats an expired
 * hold as available. The job exists so the seat map and any availability counts stop showing
 * expired holds as HELD.
 */
@Component
public class ExpiredSeatHoldReleaseJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredSeatHoldReleaseJob.class);

    private final SeatHoldService seatHoldService;

    public ExpiredSeatHoldReleaseJob(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    /** Runs every seat-hold.release-interval (default 10s) after the previous run finishes. */
    @Scheduled(
            initialDelayString = "${seat-hold.release-interval:PT10S}",
            fixedDelayString = "${seat-hold.release-interval:PT10S}")
    public void releaseExpiredHolds() {
        try {
            int released = seatHoldService.releaseExpiredHolds();
            if (released > 0) {
                log.info("Released {} expired seat hold(s)", released);
            }
        } catch (RuntimeException e) {
            // Never let one failed run stop the schedule; the next run retries.
            log.error("Failed to release expired seat holds; will retry on the next run", e);
        }
    }
}