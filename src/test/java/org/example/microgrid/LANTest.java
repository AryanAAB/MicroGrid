package org.example.microgrid;

import org.example.microgrid.constants.Constants;
import org.example.microgrid.grid.Grid;
import org.example.microgrid.house.House;
import org.example.microgrid.lan.LAN;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LANTest
{
    @Test
    public void plotNetEnergyAndMoneyOverOneDay()
    {
        // Create grid and LAN
        Grid grid = new Grid(10, 5);
        LAN lan = new LAN(grid);

        // Create a house and add to LAN
        House house = new House(
                "H-PLOT",
                2.0,
                0.5,
                0.2,
                5.0,
                6.0
        );

        lan.addHouse(house);

        double totalEnergy = 0.0;

        Instant current = Instant.now();

        // Steps per day (1-minute step)
        int stepsPerDay = (int) (Constants.SEC_IN_DAY / Constants.STEP_TO_SECONDS);

        double fractionOfDay = Constants.STEP_TO_SECONDS / (double) Constants.SEC_IN_DAY;

        // Loop over one day
        for (int i = 0; i <= stepsPerDay; i++)
        {
            // Step each house
            lan.step(current, i * fractionOfDay);

            // Every 15 minutes, run market cycle
            if ((i) % (15 * 60 / Constants.STEP_TO_SECONDS) == 0)
            {
                totalEnergy += house.getIntervalConsumption() - house.getIntervalProduction();
                lan.runMarketCycle();
            }
            // Advance time
            current = current.plusSeconds((long) Constants.STEP_TO_SECONDS);
        }

        double expectedAmount = totalEnergy >= 0 ? totalEnergy * grid.buyPrice() : totalEnergy * grid.sellPrice();

        assertEquals(expectedAmount, lan.getBill(house.getHouseId()).getNetBill(grid.buyPrice(), grid.sellPrice()), 0.001);
    }
}
