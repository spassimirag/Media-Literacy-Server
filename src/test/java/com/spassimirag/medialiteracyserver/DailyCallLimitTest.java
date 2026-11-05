package com.spassimirag.medialiteracyserver;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyCallLimitTest {

    @Test
    void allowsCallsUpToTheLimitThenRefuses() {
        DailyCallLimit limit = new DailyCallLimit(2, new SettableClock(Instant.parse("2026-11-05T10:00:00Z")));

        assertTrue(limit.tryUse());
        assertTrue(limit.tryUse());
        assertFalse(limit.tryUse());
    }

    @Test
    void zeroLimitRefusesEveryCall() {
        DailyCallLimit limit = new DailyCallLimit(0, new SettableClock(Instant.parse("2026-11-05T10:00:00Z")));

        assertFalse(limit.tryUse());
    }

    @Test
    void countResetsAtMidnightUtc() {
        SettableClock clock = new SettableClock(Instant.parse("2026-11-05T23:59:00Z"));
        DailyCallLimit limit = new DailyCallLimit(1, clock);

        assertTrue(limit.tryUse());
        assertFalse(limit.tryUse());

        clock.advance(Duration.ofMinutes(2));

        assertTrue(limit.tryUse());
        assertFalse(limit.tryUse());
    }

    private static class SettableClock extends Clock {

        private Instant now;

        SettableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }
    }
}
