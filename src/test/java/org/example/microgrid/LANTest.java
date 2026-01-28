package org.example.microgrid;

import org.example.microgrid.constants.Constants;
import org.example.microgrid.grid.Grid;
import org.example.microgrid.house.House;
import org.example.microgrid.lan.*;
import org.example.microgrid.lan.Policy.NetP2PPolicy;
import org.example.microgrid.lan.Policy.TradePolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LANTest
{
    @Test
    public void plotNetEnergyAndMoneyOverOneDay()
    {
        // Create grid and LAN
        Grid grid = new Grid(10, 5);
        TradePolicy policy = new NetP2PPolicy();
        LAN lan = new LAN(grid, policy);

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

        double fractionOfDay = Constants.STEP_TO_SECONDS / Constants.SEC_IN_DAY;

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

    @Test
    void testP2PTradeClearsCorrectly()
    {
        LAN lan = new LAN(null, null);

        House h1 = new House("A", 0, 0, 0, 10, 5);
        House h2 = new House("B", 0, 0, 0, 10, 5);

        lan.addHouse(h1);
        lan.addHouse(h2);

        EnergySnapshot seller = new EnergySnapshot(
                "A", 10.0, 0.0, 5.0, 0.0
        );

        EnergySnapshot buyer = new EnergySnapshot(
                "B", 0.0, 10.0, 0.0, 10.0
        );

        List<EnergySnapshot> buyerList = new ArrayList<>();
        buyerList.add(buyer);

        List<EnergySnapshot> sellerList = new ArrayList<>();
        sellerList.add(seller);

        new NetP2PPolicy().trade(lan, sellerList, buyerList);

        Bill sellerBill = lan.getBill("A");
        Bill buyerBill = lan.getBill("B");

        assertEquals(75.0, buyerBill.getP2PCost(), 1e-9);
        assertEquals(75.0, sellerBill.getP2PRevenue(), 1e-9);
    }

    @Test
    void testNoP2PWhenPricesDoNotCross()
    {
        LAN lan = new LAN(null, null);

        lan.addHouse(new House("S", 0, 0, 0, 10, 5));
        lan.addHouse(new House("B", 0, 0, 0, 10, 5));

        EnergySnapshot seller = new EnergySnapshot(
                "S", 10.0, 0.0, 8.0, 0.0
        );

        EnergySnapshot buyer = new EnergySnapshot(
                "B", 0.0, 10.0, 0.0, 6.0
        );

        List<EnergySnapshot> buyerList = new ArrayList<>();
        buyerList.add(buyer);

        List<EnergySnapshot> sellerList = new ArrayList<>();
        sellerList.add(seller);

        new NetP2PPolicy().trade(lan, sellerList, buyerList);

        assertEquals(10.0, lan.getBill("B").getGridImported(), 1e-9);
        assertEquals(10.0, lan.getBill("S").getGridExported(), 1e-9);
    }

    @Test
    void testFairSplitAmongBuyers()
    {
        LAN lan = new LAN(null, null);

        lan.addHouse(new House("S", 0, 0, 0, 10, 5));
        lan.addHouse(new House("B1", 0, 0, 0, 10, 5));
        lan.addHouse(new House("B2", 0, 0, 0, 10, 5));
        lan.addHouse(new House("B3", 0, 9, 0, 10, 5));

        EnergySnapshot seller = new EnergySnapshot("S", 10, 0, 5, 0);

        EnergySnapshot b1 = new EnergySnapshot("B1", 0, 5, 0, 10);
        EnergySnapshot b2 = new EnergySnapshot("B2", 0, 5, 0, 10);
        EnergySnapshot b3 = new EnergySnapshot("B3", 0, 5, 0, 10);

        List<EnergySnapshot> buyerList = new ArrayList<>();
        buyerList.add(b1);
        buyerList.add(b2);
        buyerList.add(b3);

        List<EnergySnapshot> sellerList = new ArrayList<>();
        sellerList.add(seller);

        new NetP2PPolicy().trade(lan, sellerList, buyerList);

        assertEquals(7.5 * 10 / 3, lan.getBill("B1").getP2PCost(), 1e-9);
        assertEquals(7.5 * 10 / 3, lan.getBill("B1").getP2PCost(), 1e-9);
        assertEquals(7.5 * 10 / 3, lan.getBill("B3").getP2PCost(), 1e-9);

        assertEquals(0, lan.getBill("B1").getP2PRevenue(), 1e-9);
        assertEquals(0, lan.getBill("B2").getP2PRevenue(), 1e-9);
        assertEquals(0, lan.getBill("B3").getP2PRevenue(), 1e-9);

        assertEquals(5 - 10.0 / 3, lan.getBill("B1").getGridImported(), 1e-9);
        assertEquals(5 - 10.0 / 3, lan.getBill("B1").getGridImported(), 1e-9);
        assertEquals(5 - 10.0 / 3, lan.getBill("B3").getGridImported(), 1e-9);

        assertEquals(0, lan.getBill("B1").getGridExported(), 1e-9);
        assertEquals(0, lan.getBill("B2").getGridExported(), 1e-9);
        assertEquals(0, lan.getBill("B3").getGridExported(), 1e-9);

        assertEquals(75, lan.getBill("S").getP2PRevenue(), 1e-9);
        assertEquals(0, lan.getBill("S").getP2PCost(), 1e-9);
        assertEquals(0, lan.getBill("S").getGridImported(), 1e-9);
        assertEquals(0, lan.getBill("S").getGridExported(), 1e-9);
    }

    @Test
    void testCheapestSellerClearsFirst()
    {
        LAN lan = new LAN(null, null);

        lan.addHouse(new House("S1", 0, 0, 0, 10, 5));
        lan.addHouse(new House("S2", 0, 0, 0, 10, 5));
        lan.addHouse(new House("B", 0, 0, 0, 10, 5));

        EnergySnapshot s1 = new EnergySnapshot("S1", 3, 0, 4, 0);
        EnergySnapshot s2 = new EnergySnapshot("S2", 1, 0, 6, 0);
        EnergySnapshot b  = new EnergySnapshot("B", 0, 5, 0, 10);

        List<EnergySnapshot> buyerList = new ArrayList<>();
        buyerList.add(b);

        List<EnergySnapshot> sellerList = new ArrayList<>();
        sellerList.add(s1);
        sellerList.add(s2);

        new NetP2PPolicy().trade(lan, sellerList, buyerList);

        assertEquals(-21.0, lan.getBill("S1").getNetBill(30, 15), 1e-9);
        assertEquals(-8.0, lan.getBill("S2").getNetBill(30, 15), 1e-9);
        assertEquals(59.0, lan.getBill("B").getNetBill(30, 15), 1e-9);
    }

}
