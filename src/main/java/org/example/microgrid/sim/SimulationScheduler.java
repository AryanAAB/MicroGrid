package org.example.microgrid.sim;

import org.example.microgrid.house.House;
import org.example.microgrid.lan.LAN;

import java.util.List;

public class SimulationScheduler {

    private static final int MINUTES_PER_DAY = 1440;
    private static final int MARKET_INTERVAL = 15;

//    public static void runDay(LAN lan, List<House> houses) {
//
//        for (int minute = 1; minute <= MINUTES_PER_DAY; minute++) {
//
//            // 1. advance simulation
//            for (House h : houses) {
//                h.step();
//            }
//
//            // 2. run market every 15 min
//            if (minute % MARKET_INTERVAL == 0) {
//                lan.runMarketCycle();
//            }
//        }
//
//        // 3. reset daily stats
//        lan.resetDailyStats();
//    }
}
