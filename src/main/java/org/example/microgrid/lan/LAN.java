package org.example.microgrid.lan;

import org.example.microgrid.grid.Grid;
import org.example.microgrid.house.House;

import java.time.Instant;
import java.util.*;

public class LAN
{
    private final Map<String, House> houses = new HashMap<>();
    private final Map<String, Bill> bills = new HashMap<>();
    private final Grid grid;

    public LAN(Grid grid)
    {
        this.grid = grid;
    }

    public void addHouse(House house)
    {
        houses.put(house.getHouseId(), house);
        bills.put(house.getHouseId(), new Bill());
    }

    public Bill getBill(String houseId)
    {
        return bills.get(houseId);
    }

    // runs every 15 minutes
    public void runMarketCycle()
    {
        List<EnergySnapshot> sellers = new ArrayList<>();
        List<EnergySnapshot> buyers = new ArrayList<>();

        for (Map.Entry<String, House> entry : houses.entrySet())
        {
            House house = entry.getValue();

            EnergySnapshot snapshot = getEnergySnapshot(house);

            if (snapshot.isSeller()) sellers.add(snapshot);
            if (snapshot.isBuyer()) buyers.add(snapshot);

            house.resetIntervalStats();
        }

        TradePolicy.match(this, sellers, buyers);
    }

    private EnergySnapshot getEnergySnapshot(House house)
    {
        // Add threshold energy to the grid
        getBill(house.getHouseId()).addGridExport(Math.min(house.getSellThreshold(), house.getIntervalProduction()));

        // Remaining energy is available for P2P
        double production = Math.max(0, house.getIntervalProduction() - house.getSellThreshold());

        // Get the consumption in this interval
        double consumption = house.getIntervalConsumption();

        // Get the extra deficit that this house has
        double prevConsumption = Math.max(0, getBill(house.getHouseId()).getGridImported() -
                getBill(house.getHouseId()).getGridExported());

        double surplus = 0.0, deficit = 0.0;

        // If production is more, then import the entire consumption from grid
        // and export the entire consumption and previous consumption to grid
        if (production >= consumption + prevConsumption)
        {
            getBill(house.getHouseId()).addGridImport(consumption);
            getBill(house.getHouseId()).addGridExport(consumption + prevConsumption);

            surplus = production - consumption - prevConsumption;
        }
        // otherwise import and export the production value
        else
        {
            getBill(house.getHouseId()).addGridImport(production);
            getBill(house.getHouseId()).addGridExport(production);

            deficit = consumption - production;
        }

        return new EnergySnapshot(
                house.getHouseId(),
                surplus,
                deficit,
                house.getSellingPrice(),
                house.getCostPrice()
        );
    }

    public Grid getGrid()
    {
        return this.grid;
    }

    // runs every 1 minute
    public void step(Instant timestamp, double fractionOfDay)
    {
        for (Map.Entry<String, House> entry : houses.entrySet())
        {
            entry.getValue().step(timestamp, fractionOfDay);
        }
    }

    // call every 1 month
    public Map<String, Bill> getBills()
    {
        return Collections.unmodifiableMap(bills);
    }

    // call every 1 month
    public void clearBills()
    {
        bills.values().forEach(Bill::clear);
    }
}
