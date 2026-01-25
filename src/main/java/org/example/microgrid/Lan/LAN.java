package org.example.microgrid.Lan;

import org.example.microgrid.house.House;
import org.example.microgrid.Grid.Grid;

import java.util.*;

public class LAN {

    private final List<House> houses = new ArrayList<>();
    private final Map<String, Double> bills = new HashMap<>();
    private final Grid grid;

    private final TradePolicy tradePolicy = new TradePolicy();

    public LAN(Grid grid) {
        this.grid = grid;
    }

    public void addHouse(House house) {
        houses.add(house);
        bills.put(house.getHouseId(), 0.0);
        tradePolicy.registerHouse(house);
    }

    public void runMarketCycle() {
        tradePolicy.runMarketCycle(this);
    }

    /* accessors used by TradePolicy */

    public List<House> getHouses() {
        return houses;
    }

    public Grid getGrid() {
        return grid;
    }

    public void addBill(String houseId, double delta) {
        bills.put(houseId, bills.get(houseId) + delta);
    }

    public double getBill(String houseId) {
        return bills.get(houseId);
    }
}