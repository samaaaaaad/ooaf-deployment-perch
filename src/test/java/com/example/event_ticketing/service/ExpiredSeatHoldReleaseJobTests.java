package com.example.event_ticketing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpiredSeatHoldReleaseJobTests {

    private SeatHoldService seatHoldService;
    private ExpiredSeatHoldReleaseJob job;

    @BeforeEach
    void setUp() {
        seatHoldService = mock(SeatHoldService.class);
        job = new ExpiredSeatHoldReleaseJob(seatHoldService);
    }

    @Test
    void eachRunAsksTheServiceToReleaseExpiredHolds() {
        when(seatHoldService.releaseExpiredHolds()).thenReturn(2);

        job.releaseExpiredHolds();

        verify(seatHoldService).releaseExpiredHolds();
    }

    @Test
    void aFailedRunDoesNotPropagateSoTheScheduleKeepsGoing() {
        when(seatHoldService.releaseExpiredHolds()).thenThrow(new RuntimeException("database unavailable"));

        assertThatCode(() -> job.releaseExpiredHolds()).doesNotThrowAnyException();
    }
}
