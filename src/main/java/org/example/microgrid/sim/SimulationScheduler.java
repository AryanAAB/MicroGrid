package org.example.microgrid.sim;

import org.example.microgrid.lan.LAN;
import org.example.microgrid.house.House;

import java.util.List;

public class SimulationScheduler {

    private static final int MINUTES_PER_DAY = 1440;

    public static void runDay(LAN lan, List<House> houses) {
        for (int i = 0; i < MINUTES_PER_DAY; i++) {
            for (House h : houses) {
                h.step();
            }
        }
        lan.endOfDaySettlement();
    }
}
