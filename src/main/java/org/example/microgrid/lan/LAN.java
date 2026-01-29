package org.example.microgrid.lan;

import org.example.microgrid.grid.Grid;
import org.example.microgrid.house.House;
import org.example.microgrid.lan.policy.NetP2PPolicy;
import org.example.microgrid.lan.policy.TradePolicy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LAN
{
    private final Map<String, House> houses = new HashMap<>();
    private final Map<String, Bill> bills = new HashMap<>();
    private final Grid grid;
    private final TradePolicy tradePolicy;

    public LAN(Grid grid, TradePolicy tradePolicy)
    {
        this.grid = grid;
        this.tradePolicy = tradePolicy;
    }

    public LAN(Grid grid)
    {
        this(grid, new NetP2PPolicy());
    }

    public LAN()
    {
        this(new Grid(10, 3));
    }

    public void addHouse(House house)
    {
        houses.put(house.getHouseId(), house);
        bills.put(house.getHouseId(), new Bill(grid));
    }

    public void runMarketCycle()
    {
        List<EnergySnapshot> sellers = new ArrayList<>();
        List<EnergySnapshot> buyers = new ArrayList<>();

        for (Map.Entry<String, House> entry : houses.entrySet())
        {
            House house = entry.getValue();

            EnergySnapshot snapshot = tradePolicy.getEnergySnapshot(this, house);

            if (snapshot.isSeller()) sellers.add(snapshot);
            else buyers.add(snapshot);

            house.resetIntervalStats();
        }

        tradePolicy.trade(this, sellers, buyers);
    }

    public void step(Instant timestamp, double fractionOfDay)
    {
        houses.values().forEach(h -> h.step(timestamp, fractionOfDay));
    }

    public Bill getBill(String houseId)
    {
        return bills.get(houseId);
    }

    public List<Bill> getBills()
    {
        return new ArrayList<>(bills.values());
    }

    public Grid getGrid()
    {
        return grid;
    }

    public void resetStats()
    {
        bills.values().forEach(Bill::clear);
    }
}
