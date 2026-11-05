package com.spassimirag.medialiteracyserver;

import java.time.Clock;
import java.time.LocalDate;

public class DailyCallLimit {

    private final int maxCallsPerDay;
    private final Clock clock;

    private LocalDate currentDay;
    private int callsToday;

    public DailyCallLimit(int maxCallsPerDay, Clock clock) {
        this.maxCallsPerDay = maxCallsPerDay;
        this.clock = clock;
        this.currentDay = LocalDate.now(clock);
        this.callsToday = 0;
    }

    public synchronized boolean tryUse() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(currentDay)) {
            currentDay = today;
            callsToday = 0;
        }
        if (callsToday >= maxCallsPerDay) {
            return false;
        }
        callsToday++;
        return true;
    }
}
