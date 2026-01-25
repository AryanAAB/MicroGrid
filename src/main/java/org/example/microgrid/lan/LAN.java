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
    private Map<String, House> houses = new HashMap<>();
    private Map<String, Double> bills = new HashMap<>();
    private Map<String, Double> residualDemand = new HashMap<>();
    private Map<String, Double> residualSupply = new HashMap<>();
    private final Grid grid;

    public LAN(Grid grid)
    {
        this.grid = grid;
    }

    public void addHouse(House house)
    {
        houses.put(house.getHouseId(), house);
    }

    public void addBill(String id, double amt) {
        bills.put(id, amt + bills.get(id));
    }

    public double generateBill(String houseId) {
        double finalBill = bills.get(houseId);
        if (residualDemand.get(houseId) >= residualSupply.get(houseId)) {
            finalBill += grid.buyFromGrid(residualDemand.get(houseId) - residualSupply.get(houseId));
        }
        else {
            finalBill -= grid.sellToGrid(residualSupply.get(houseId) - residualDemand.get(houseId));
        }
        bills.put(houseId, 0.0);
        residualSupply.put(houseId, 0.0);
        residualDemand.put(houseId, 0.0);
        return finalBill;
    }

    public void addResidualDemand(String houseId, double value) {
        residualDemand.put(houseId, value + residualDemand.get(houseId));
    }
    public void addResidualSupply(String houseId, double value) {
        residualSupply.put(houseId, value + residualSupply.get(houseId));
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

        TradePolicy.match(this, sellers, buyers, grid);
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
