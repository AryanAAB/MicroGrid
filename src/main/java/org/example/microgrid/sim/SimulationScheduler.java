package org.example.microgrid.sim;

import org.example.microgrid.lan.LAN;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SimulationScheduler {
    private static final int MINUTES_PER_DAY = 1440;
    private static final int MARKET_INTERVAL = 15;

    public static void runDay(LAN lan) {
        // Start simulation at the beginning of the current day (00:00)
        Instant simTime = Instant.now().truncatedTo(ChronoUnit.DAYS);

        for (int minute = 1; minute <= MINUTES_PER_DAY; minute++) {
            double fractionOfDay = (double) minute / MINUTES_PER_DAY;

            // 1. Advance the simulation state
            // Each step represents 1 minute (60 seconds)
            lan.step(simTime.plusSeconds(minute * 60L), fractionOfDay);

            // 2. Market cycle every 15 minutes
            if (minute % MARKET_INTERVAL == 0) {
                lan.runMarketCycle();
            }
        }

        // 3. Optional: In a real simulation, you'd move this to a Monthly Scheduler
         lan.resetDailyStats(); 
    }
}