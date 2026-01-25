package org.example.microgrid.lan;

import org.example.microgrid.house.House;
import org.example.microgrid.grid.Grid;

import java.util.ArrayList;
import java.util.List;

public class LAN {

    private final List<House> houses = new ArrayList<>();
    private final Grid grid;

    public LAN(Grid grid) {
        this.grid = grid;
    }

    public void addHouse(House house) {
        houses.add(house);
    }

    // runs every 15 minutes
    public void runMarketCycle() {

        List<EnergySnapshot> sellers = new ArrayList<>();
        List<EnergySnapshot> buyers = new ArrayList<>();

        for (House house : houses) {

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
        }

        TradePolicy.match(sellers, buyers, grid);

        // reset interval stats after market
        for (House house : houses) {
            house.resetIntervalStats();
        }
    }

    // called once per day
    public void resetDailyStats() {
        for (House house : houses) {
            house.resetDailyStats();
        }
    }
}
