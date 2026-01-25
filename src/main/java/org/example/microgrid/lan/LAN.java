package org.example.microgrid.lan;

import org.example.microgrid.grid.Grid;
import org.example.microgrid.house.House;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LAN
{
    private final Map<String, House> houses = new HashMap<>();
    private final Grid grid;

    public LAN(Grid grid)
    {
        this.grid = grid;
    }

    public void addHouse(House house)
    {
        houses.put(house.getHouseId(), house);
    }

    // runs every 15 minutes
    public void runMarketCycle()
    {
        List<EnergySnapshot> sellers = new ArrayList<>();
        List<EnergySnapshot> buyers = new ArrayList<>();

        for (Map.Entry<String, House> entry : houses.entrySet())
        {
            House house = entry.getValue();

            EnergySnapshot snapshot =
                    new EnergySnapshot(
                            house.getHouseId(),
                            house.getIntervalProduction(),
                            house.getIntervalConsumption(),
                            house.getSellingPrice(),
                            house.getCostPrice()
                    );

            if (snapshot.isSeller()) sellers.add(snapshot);
            if (snapshot.isBuyer()) buyers.add(snapshot);

            house.resetIntervalStats();
        }

        TradePolicy.match(sellers, buyers, grid);
    }

    // runs every 1 minute
    public void step(Instant timestamp, double fractionOfDay)
    {
        for(Map.Entry<String, House> entry : houses.entrySet())
        {
            entry.getValue().step(timestamp, fractionOfDay);
        }
    }
}
