package org.example.microgrid.lan;

import org.example.microgrid.house.House;

import java.util.ArrayList;
import java.util.List;

public class LAN {

    private final List<House> houses = new ArrayList<>();

    public void addHouse(House house) {
        houses.add(house);
    }

    public void endOfDaySettlement() {

        List<EnergySnapshot> sellers = new ArrayList<>();
        List<EnergySnapshot> buyers = new ArrayList<>();

        for (House house : houses) {

            EnergySnapshot snapshot =
                    new EnergySnapshot(
                            house.getHouseId(),
                            house.getDailyProduction(),
                            house.getDailyConsumption(),
                            house.getSellingPrice(),
                            house.getCostPrice()
                    );

            if (snapshot.isSeller()) sellers.add(snapshot);
            if (snapshot.isBuyer()) buyers.add(snapshot);
        }

        TradePolicy.match(sellers, buyers);

        // reset AFTER settlement
        for (House house : houses) {
            house.resetDailyStats();
        }
    }
}
