package org.example.microgrid.lan;

import org.example.microgrid.grid.Grid;
import org.example.microgrid.house.House;
import org.example.microgrid.house.Location;

import java.time.Instant;
import java.util.*;

public class LAN {
    private final Map<String, House> houses = new HashMap<>();
    private final Map<String, Bill> bills = new HashMap<>();
    private final Grid grid;
    private final Location transformerLocation;
    private final double coverageRadius;

    public LAN(Grid grid, Location transformerLocation, double coverageRadius) {
        this.grid = grid;
        this.transformerLocation = transformerLocation;
        this.coverageRadius = coverageRadius;
    }

    public void addHouse(House house) {
        houses.put(house.getHouseId(), house);
        bills.put(house.getHouseId(), new Bill());
    }

    public void runMarketCycle() {
        List<EnergySnapshot> sellers = new ArrayList<>();
        List<EnergySnapshot> buyers = new ArrayList<>();

        for (House house : houses.values()) {
            EnergySnapshot snapshot = createSnapshot(house);
            if (snapshot.isSeller()) sellers.add(snapshot);
            else if (snapshot.isBuyer()) buyers.add(snapshot);
            
            house.resetIntervalStats();
        }

        TradePolicy.match(this, sellers, buyers);
    }

    private EnergySnapshot createSnapshot(House house) {
        Bill bill = getBill(house.getHouseId());
        double production = house.getIntervalProduction();
        double consumption = house.getIntervalConsumption();

        double surplus = Math.max(0, production - consumption);
        double deficit = Math.max(0, consumption - production);

        // Export to grid only if it hits threshold
        double gridExport = Math.min(surplus, house.getSellThreshold());
        bill.addGridExport(gridExport);
        surplus -= gridExport;

        // NOTE: We do NOT add Grid Import here. TradePolicy will do it for remaining deficit.
        return new EnergySnapshot(
                house.getHouseId(),
                surplus,
                deficit,
                house.getSellingPrice(),
                house.getCostPrice()
        );
    }

    public void step(Instant timestamp, double fractionOfDay) {
        houses.values().forEach(h -> h.step(timestamp, fractionOfDay));
    }

    public Bill getBill(String houseId) { return bills.get(houseId); }
    public Grid getGrid() { return grid; }
    public void resetDailyStats() { bills.values().forEach(Bill::clear); }
}